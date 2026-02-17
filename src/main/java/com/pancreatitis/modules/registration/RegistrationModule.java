package com.pancreatitis.modules.registration;

import com.pancreatitis.models.Doctor;
import com.pancreatitis.models.RegistrationForm;

import com.pancreatitis.modules.cloudstorage.CloudStorageModule;
import com.pancreatitis.modules.database.DatabaseModule;
import com.pancreatitis.modules.safety.SafetyModule;

import javax.crypto.SecretKey;

public class RegistrationModule {
    private static CloudStorageModule cloudStorageModule;
    private static DatabaseModule databaseModule;
    private static SafetyModule safetyModule;
    private static RegistrationModule instance;
    private RegistrationModule(){
        cloudStorageModule = CloudStorageModule.getInstance();
        safetyModule = SafetyModule.getInstance();
        databaseModule = DatabaseModule.getInstance();
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
    public boolean acceptRegistrationRequest(RegistrationForm registrationForm) {
        try {
            SecretKey key = safetyModule.generateKey();

            String ADMIN_PASSWORD = safetyModule.getAdminPassword();
            String user_password = registrationForm.getPassword();

            byte[] encrypt_amdin_key = safetyModule.encryptKey(key, ADMIN_PASSWORD);
            byte[] encrypt_user_key = safetyModule.encryptKey(key, user_password);

            String login = registrationForm.getLogin();
            cloudStorageModule.createUserFolder(login);
            cloudStorageModule.uploadUserKey(login, encrypt_user_key, "key_user");
            cloudStorageModule.uploadUserKey(login, encrypt_amdin_key, "key_admin");
            cloudStorageModule.uploadUserInfo(registrationForm);

            Doctor doctor = new Doctor();
            doctor.setFio(registrationForm.getFullName());
            databaseModule.insertDoctor(doctor);

            cloudStorageModule.deleteRegistrationRequest(login);

            return true;
        } catch (Exception e) {
            return false;
        }
    }
    public boolean rejectRegistrationRequest(RegistrationForm registrationForm){
        String login = registrationForm.getLogin();
        return cloudStorageModule.deleteRegistrationRequest(login);
    }
}

