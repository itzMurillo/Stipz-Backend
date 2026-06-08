package br.com.stipz.controller;

import br.com.stipz.service.BackupAuditoriaService;
import br.com.stipz.service.BackupAuditoriaService.BackupGerado;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/backups/auditoria")
public class BackupAuditoriaController {

    private final BackupAuditoriaService backupAuditoriaService;

    public BackupAuditoriaController(BackupAuditoriaService backupAuditoriaService) {
        this.backupAuditoriaService = backupAuditoriaService;
    }

    @PostMapping("/gerar")
    public Map<String, String> gerarBackup() {
        BackupGerado backup = backupAuditoriaService.gerarBackupManual();

        return Map.of(
                "mensagem", "Backup de auditoria gerado com sucesso",
                "arquivoSql", backup.arquivoSql(),
                "arquivoRelatorio", backup.arquivoRelatorio()
        );
    }
}
