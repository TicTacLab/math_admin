TRUNCATE malt.configuration;

INSERT INTO
       malt.configuration (config)
VALUES
	('{"cache-table":"malt.workbook","hosts":"localhost,","password":"dem-pass","username":"dem","rest-port":3000,"calculation-malt-node":"10.1.4.163","malt-nodes":"localhost,10.1.4.163","malt-reload-model-url":"/model/refresh","session-ttl":30000,"calculation-malt-port":"3000"}');


