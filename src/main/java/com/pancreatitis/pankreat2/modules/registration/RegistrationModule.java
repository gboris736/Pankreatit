package com.pancreatitis.pankreat2.modules.registration;

import com.pancreatitis.pankreat2.models.RegistrationForm;

import com.pancreatitis.pankreat2.modules.authorization.AuthorizationModule;
import com.pancreatitis.pankreat2.modules.cloudstorage.CloudStorageModule;

public class RegistrationModule {
    private static CloudStorageModule cloudStorageModule;
    private static RegistrationModule instance;
    private RegistrationModule(){
        cloudStorageModule = CloudStorageModule.getInstance();
    }
    public static RegistrationModule getInstance(){
        if (instance == null) {
            instance = new RegistrationModule();
        }
        return instance;
    }
    public boolean submitRegistrationRequest(RegistrationForm registrationForm) throws Exception {
        return cloudStorageModule.uploadRegistrationRequest(registrationForm);
    }
}

