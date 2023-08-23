CREATE DATABASE IF NOT EXISTS uciplink;

USE uciplink;

-- ------------------------------------
--  Table structure for `ersinstall`
-- ------------------------------------
DROP TABLE IF EXISTS `ersinstall`;

CREATE TABLE `ersinstall` (
  `VersionKey` smallint(6) NOT NULL AUTO_INCREMENT,
  `Version` varchar(20) NOT NULL,
  `Status` tinyint(4) NOT NULL DEFAULT '0',
  `Script` varchar(200) NOT NULL,
  `last_modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`VersionKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `uciplink_native_reference_sequence`;

CREATE TABLE `uciplink_native_reference_sequence` (
  `seqNo` int(11) NOT NULL,
   PRIMARY KEY (`seqNo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `uciplink_native_reference_sequence` (`seqNo`) VALUES (1);