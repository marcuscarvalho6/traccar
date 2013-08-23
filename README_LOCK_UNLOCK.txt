
É necessário cria a tabela para funcionamento do BLOQUEIO e DESBLOQUEIO.

CREATE TABLE IF NOT EXISTS `command` (
  `imei` varchar(17) NOT NULL,
  `command` varchar(80) NOT NULL,
  `date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`imei`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 ROW_FORMAT=DYNAMIC COMMENT='Dados de comandos para envio ao GPS';

Só testei o funcionamento no mysql.

Ao inserir um registro na tabela com imei e command como: BLOQUEAR (LOCK) ou DESBLOQUEAR (UNLOCK) o traccar socket ler esta informação e enviar ao dispositivo.
