package com.toyota.tsc.notificationhub;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.toyota.tsc.notificationhub.commons.PropertiesUtil;

import jp.toyota.res.common.utils.ALJToken;

@Component
public class InitializeRunner implements ApplicationRunner {

    /**
     * KeyVault共通部品.
     */
    @Autowired
    private PropertiesUtil keyVaultUtil;

    /**
     * ALJトークンライブラリ.
     */
    private final ALJToken aljToken;

    InitializeRunner(ALJToken aljToken) {
        this.aljToken = aljToken;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        initialize();
    }

    private void initialize() {
        try {
            String hostName = keyVaultUtil.getDatasourceHost();
            String portNo = keyVaultUtil.getDatasourcePort();
            String dbName = keyVaultUtil.getDatasourceName();
            String userName = keyVaultUtil.getDatasourceUsername();
            String password = keyVaultUtil.getDatasourcePassword();

            aljToken.init(hostName, portNo, dbName, userName, password);
        } catch (Exception ex) {
            // ログ出力
        }
    }
}
