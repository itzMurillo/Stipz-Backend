package br.com.stipz.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class BackupAuditoriaService {

    private static final DateTimeFormatter FORMATO_ARQUIVO = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;

    public BackupAuditoriaService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${stipz.backup.auditoria.ativo:true}")
    private boolean ativo;

    @Value("${stipz.backup.auditoria.pasta:backups/auditoria}")
    private String pastaBackup;

    @Value("${stipz.backup.auditoria.manter-arquivos:7}")
    private int manterArquivos;

    @Value("${stipz.backup.auditoria.pg-dump:pg_dump}")
    private String pgDump;

    @Scheduled(cron = "${stipz.backup.auditoria.cron:0 0 2 * * *}")
    public void gerarBackupDiario() {
        if (!ativo) {
            return;
        }

        try {
            criarBackup();
            apagarBackupsAntigos();
        } catch (Exception ex) {
            System.err.println("Erro ao gerar backup de auditoria: " + ex.getMessage());
        }
    }

    public BackupGerado gerarBackupManual() {
        try {
            BackupGerado backup = criarBackup();
            apagarBackupsAntigos();
            return backup;
        } catch (Exception ex) {
            throw new IllegalStateException("Erro ao gerar backup de auditoria: " + ex.getMessage());
        }
    }

    private BackupGerado criarBackup() throws IOException, InterruptedException {
        DadosBanco dadosBanco = extrairDadosBanco();
        Files.createDirectories(Path.of(pastaBackup));

        String dataArquivo = LocalDateTime.now().format(FORMATO_ARQUIVO);
        String nomeArquivo = "auditoria_" + dataArquivo + ".sql";
        Path arquivoSql = Path.of(pastaBackup, nomeArquivo);

        ProcessBuilder processBuilder = new ProcessBuilder(
                pgDump,
                "-h", dadosBanco.host(),
                "-p", dadosBanco.porta(),
                "-U", username,
                "-d", dadosBanco.nomeBanco(),
                "--no-owner",
                "--table=public.revinfo",
                "--table=public.*_aud",
                "-f", arquivoSql.toString()
        );

        processBuilder.environment().put("PGPASSWORD", password);
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);

        Process processo = processBuilder.start();
        int codigoSaida = processo.waitFor();

        if (codigoSaida != 0) {
            Files.deleteIfExists(arquivoSql);
            throw new IllegalStateException("pg_dump finalizou com codigo " + codigoSaida);
        }

        Path arquivoRelatorio = Path.of(pastaBackup, "auditoria_" + dataArquivo + ".txt");
        gerarRelatorioLegivel(arquivoRelatorio);

        return new BackupGerado(
                arquivoSql.toAbsolutePath().toString(),
                arquivoRelatorio.toAbsolutePath().toString()
        );
    }

    private void gerarRelatorioLegivel(Path arquivoRelatorio) throws IOException {
        List<Map<String, Object>> registros = jdbcTemplate.queryForList("""
                select rev, revtstmp, descricao from (
                    select u.rev, r.revtstmp,
                        'Usuario #' || u.id || ': ' || case u.revtype when 0 then 'CRIADO' when 1 then 'ALTERADO' else 'EXCLUIDO' end ||
                        ' - ' || coalesce(u.nome, '') || ' (' || coalesce(u.email, '') || '), perfil ' || coalesce(u.perfil, '') as descricao
                    from usuario_aud u join revinfo r on r.rev = u.rev

                    union all

                    select s.rev, r.revtstmp,
                        'Sala #' || s.id || ': ' || case s.revtype when 0 then 'CRIADA' when 1 then 'ALTERADA' else 'EXCLUIDA' end ||
                        ' - ' || coalesce(s.nome, '') || ', capacidade ' || coalesce(s.capacidade::text, '') as descricao
                    from sala_aud s join revinfo r on r.rev = s.rev

                    union all

                    select tr.rev, r.revtstmp,
                        'Tipo de recurso #' || tr.id || ': ' || case tr.revtype when 0 then 'CRIADO' when 1 then 'ALTERADO' else 'EXCLUIDO' end ||
                        ' - ' || coalesce(tr.nome, '') || ', categoria ' || coalesce(tr.categoria, '') as descricao
                    from tipo_recurso_aud tr join revinfo r on r.rev = tr.rev

                    union all

                    select rc.rev, r.revtstmp,
                        'Recurso #' || rc.id || ': ' || case rc.revtype when 0 then 'CRIADO' when 1 then 'ALTERADO' else 'EXCLUIDO' end ||
                        ' - ' || coalesce(rc.nome, '') || ', quantidade ' || coalesce(rc.quantidade::text, '') ||
                        ', sala #' || coalesce(rc.id_sala::text, 'sem sala') as descricao
                    from recurso_aud rc join revinfo r on r.rev = rc.rev

                    union all

                    select rv.rev, r.revtstmp,
                        'Reserva #' || rv.id || ': ' || case rv.revtype when 0 then 'CRIADA' when 1 then 'ALTERADA' else 'EXCLUIDA' end ||
                        ' - status ' || coalesce(rv.status, '') || ', sala #' || coalesce(rv.id_sala::text, '') ||
                        ', usuario #' || coalesce(rv.id_usuario::text, '') || ', inicio ' || coalesce(rv.data_inicio::text, '') ||
                        ', fim ' || coalesce(rv.data_fim::text, '') ||
                        ', cadeiras extras ' || case when coalesce(rv.cadeiras_extras, false) then 'sim' else 'nao' end ||
                        ', quantidade ' || coalesce(rv.quantidade_cadeiras::text, '0') as descricao
                    from reserva_aud rv join revinfo r on r.rev = rv.rev

                    union all

                    select rr.rev, r.revtstmp,
                        'Vinculo de recurso #' || rr.id || ' da reserva #' || coalesce(rr.id_reserva::text, '') || ': ' ||
                        case rr.revtype when 0 then 'CRIADO' when 1 then 'ALTERADO' else 'EXCLUIDO' end ||
                        ' - recurso #' || coalesce(rr.id_recurso::text, '') || ', quantidade ' || coalesce(rr.quantidade::text, '') as descricao
                    from reserva_recurso_aud rr join revinfo r on r.rev = rr.rev

                    union all

                    select e.rev, r.revtstmp,
                        'Evento #' || e.id || ': ' || case e.revtype when 0 then 'CRIADO' when 1 then 'ALTERADO' else 'EXCLUIDO' end ||
                        ' - ' || coalesce(e.nome, '') || ', inicio ' || coalesce(e.data_inicio::text, '') ||
                        ', fim ' || coalesce(e.data_fim::text, '') as descricao
                    from evento_aud e join revinfo r on r.rev = e.rev
                ) auditoria
                order by rev, descricao
                """);

        List<String> linhas = new ArrayList<>();
        linhas.add("RELATORIO DE AUDITORIA - STIPZ");
        linhas.add("Gerado em: " + LocalDateTime.now().format(FORMATO_DATA));
        linhas.add("");

        if (registros.isEmpty()) {
            linhas.add("Nenhum registro de auditoria encontrado.");
        }

        for (Map<String, Object> registro : registros) {
            long timestamp = ((Number) registro.get("revtstmp")).longValue();
            String data = Instant.ofEpochMilli(timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime()
                    .format(FORMATO_DATA);

            linhas.add("Revisão " + registro.get("rev") + " - " + data);
            linhas.add(String.valueOf(registro.get("descricao")));
            linhas.add("");
        }

        Files.write(arquivoRelatorio, linhas, StandardCharsets.UTF_8);
    }

    private void apagarBackupsAntigos() throws IOException {
        Path pasta = Path.of(pastaBackup);

        if (!Files.exists(pasta)) {
            return;
        }

        List<Path> backupsSql;

        try (Stream<Path> arquivos = Files.list(pasta)) {
            backupsSql = arquivos
                    .filter(Files::isRegularFile)
                    .filter(arquivo -> arquivo.getFileName().toString().startsWith("auditoria_"))
                    .filter(arquivo -> arquivo.getFileName().toString().endsWith(".sql"))
                    .sorted(Comparator.comparing(this::ultimaModificacao).reversed())
                    .toList();
        }

        for (int i = manterArquivos; i < backupsSql.size(); i++) {
            Path arquivoSql = backupsSql.get(i);
            Path arquivoTxt = Path.of(arquivoSql.toString().replace(".sql", ".txt"));
            Files.deleteIfExists(arquivoSql);
            Files.deleteIfExists(arquivoTxt);
        }
    }

    private File ultimaModificacao(Path arquivo) {
        return arquivo.toFile();
    }

    private DadosBanco extrairDadosBanco() {
        String url = datasourceUrl.replace("jdbc:postgresql://", "");
        String[] partes = url.split("/", 2);
        String[] hostPorta = partes[0].split(":", 2);

        String host = hostPorta[0];
        String porta = hostPorta.length > 1 ? hostPorta[1] : "5432";
        String nomeBanco = partes[1].split("\\?", 2)[0];

        return new DadosBanco(host, porta, nomeBanco);
    }

    private record DadosBanco(String host, String porta, String nomeBanco) {
    }

    public record BackupGerado(String arquivoSql, String arquivoRelatorio) {
    }
}
