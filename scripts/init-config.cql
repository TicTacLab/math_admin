TRUNCATE malt.configuration;

INSERT INTO
       malt.configuration (config)
VALUES
	('{"cache-table":"malt.workbook","hosts":"localhost,","password":"dem-pass","username":"dem","rest-port":3000,"calculation-malt-node":"localhost","malt-nodes":"localhost","malt-reload-model-url":"/model/refresh","session-ttl":30000,"calculation-malt-port":"3000"}');


