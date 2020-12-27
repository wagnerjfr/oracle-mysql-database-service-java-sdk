package com.oci.mds.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.mysql.cloud.Joci;
import com.oracle.mysql.cloud.JociClients;
import com.oracle.mysql.cloud.JociException;
import com.oracle.mysql.cloud.JociFactory;
import com.oracle.mysql.cloud.configuration.Bindings;
import com.oracle.mysql.cloud.configuration.CloudConfiguration;
import com.oracle.mysql.cloud.core.JociIdentity;
import com.oracle.mysql.cloud.maas.JociConfiguration;
import com.oracle.mysql.cloud.maas.JociDbBackups;
import com.oracle.mysql.cloud.maas.JociDbSystem;
import io.dropwizard.Configuration;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.util.Properties;

@Slf4j
@Getter
@Setter
@JsonIgnoreProperties({
    "configWithProfile",
    "provider",
    "identityClient",
    "configurationClient",
    "dbSystemClient",
    "dbBackupsClient",
    "analyticsClient",
    "availabilityDomain",
    "region",
    "tenantId"
    })

public class OciConfiguration extends Configuration {

    private String regionHost;
    private String stage;
    private String realm;

    private String availabilityDomain;
    private String logicalADName;

    protected Properties props = new Properties();
    protected String ociConfigPath = null;
    protected String profileName = null;
    protected String clientTenancyId = null;
    protected String mysqlClientEndpoint;

    protected ConfigFileReader.ConfigFile configWithProfile = null;
    protected BasicAuthenticationDetailsProvider provider = null;

    //protected JociIdentity identityClient;
    //protected JociConfiguration configurationClient;
    protected JociDbSystem dbSystemClient;
    protected JociDbBackups dbBackupsClient;

    OciConfiguration() {
        try {
            ociConfigPath = System.getProperty("testConfig");
            if (ociConfigPath == null) {
                // Try to load the desktop developer default
                ociConfigPath = System.getProperty("user.home") + "/.oci/config";
            }
            log.info("loading configuration from {}", ociConfigPath);
            props.load(new FileInputStream(ociConfigPath));

            profileName = System.getProperty("profileName");
            if (profileName == null) {
                profileName = "DEFAULT";
            }
            log.info("using profile {}", profileName);

            ConfigProfile configProfile = new ConfigProfile(ociConfigPath, profileName);
            configWithProfile = configProfile.getConfigWithProfile();
            provider = configProfile.getProvider();

            clientTenancyId = configWithProfile.get("tenancy");

        } catch (Exception ex) {
            log.error(ex.getMessage());
        }
    }

    public void setUpClients(String mysqlClientEndpoint, String compartmentId) throws JociException {
        CloudConfiguration cloudConfiguration = new CloudConfiguration(ociConfigPath, provider, mysqlClientEndpoint, availabilityDomain, compartmentId, null);

        // JAVA
        //Bindings bindings = new Bindings("", "", "");
        //Joci joci = JociFactory.instantiate(cloudConfiguration, bindings, JociClients.JAVA_SDK);

        // CLI
        String cliPath = System.getProperty("user.home") + "/bin/oci";
        String execPath = System.getProperty("user.home") + "/joci-cli/test/";
        Bindings bindings = new Bindings(cliPath, execPath, execPath);
        Joci joci = JociFactory.instantiate(cloudConfiguration, bindings, JociClients.CLI);

        dbBackupsClient = joci.maas().getDbBackups();
        dbSystemClient = joci.maas().getDbSystem();
        //identityClient = joci.core().identity();
        //configurationClient = joci.maas().getConfiguration();
    }
}
