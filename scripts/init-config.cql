INSERT INTO
       cluster_store.configuration (profile, config)
VALUES
       ('dev', '{:dev {:scheduler {:calc-timeout 1500, :workers 30}, :hazelcast {:cache-on? false, :hosts ["localhost"], :workbook-map "malt.workbook", :pass "dem-pass", :user "dem"}, :session {:expire 30000}}}');

INSERT INTO
       cluster_store.configuration (profile, config)
VALUES
       ('prod', '{:prod {:scheduler {:calc-timeout 2000, :workers 30}, :hazelcast {:cache-on? true, :hosts ["localhost"], :workbook-map "malt.workbook", :pass "malt-pass", :user "malt"}, :session {:expire 1800000}}}');
