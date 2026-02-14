package com.pancreatitis.pankreat2.modules.registration;

import com.pancreatitis.pankreat2.models.Doctor;
import com.pancreatitis.pankreat2.models.RegistrationForm;

import com.pancreatitis.pankreat2.modules.cloudstorage.CloudStorageModule;
import com.pancreatitis.pankreat2.modules.database.DatabaseModule;
import com.pancreatitis.pankreat2.modules.safety.SafetyModule;

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
            cloudStorageModule.createFolder("users",login);
            cloudStorageModule.uploadUserKey(login, encrypt_user_key, "key_user");
            cloudStorageModule.uploadUserKey(login, encrypt_amdin_key, "key_admin");
            cloudStorageModule.uploadUserInfo(registrationForm);

            Doctor doctor = new Doctor();
            doctor.setFio(registrationForm.getFullName());
            databaseModule.insertDoctor(doctor);

            deleteRegistrationRequest(registrationForm);

            return true;
        } catch (Exception e) {
            return false;
        }
    }
    public boolean deleteRegistrationRequest(RegistrationForm registrationForm){
        String login = registrationForm.getLogin();
        return cloudStorageModule.deleteRegistrationRequest(login);
    }
}

