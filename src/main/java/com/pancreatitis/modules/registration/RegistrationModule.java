package com.pancreatitis.modules.registration;

import com.pancreatitis.models.Doctor;
import com.pancreatitis.models.RegistrationForm;

import com.pancreatitis.modules.bip39.Bip39Encoder;
import com.pancreatitis.modules.cloudstorage.CloudStorageModule;
import com.pancreatitis.modules.database.DatabaseModule;
import com.pancreatitis.modules.localstorage.LocalStorageModule;
import com.pancreatitis.modules.safety.SafetyModule;

import javax.crypto.SecretKey;

public class RegistrationModule {
    private static CloudStorageModule cloudStorageModule;
    private static LocalStorageModule localStorageModule;
    private static DatabaseModule databaseModule;
    private static SafetyModule safetyModule;
    private static RegistrationModule instance;
    private RegistrationModule(){
        safetyModule = SafetyModule.getInstance();
        databaseModule = DatabaseModule.getInstance();
    }
    public static RegistrationModule getInstance(){
        if (instance == null) {
            instance = new RegistrationModule();
        }
        return instance;
    }

    public LocalStorageModule getLocalStorageModule() {
        if (localStorageModule == null) {
            localStorageModule = LocalStorageModule.getInstance();
        }
        return localStorageModule;
    }

    public CloudStorageModule getCloudStorageModule() {
        if (cloudStorageModule == null) {
            cloudStorageModule = CloudStorageModule.getInstance();
        }
        return cloudStorageModule;
    }

    public boolean submitRegistrationRequest(RegistrationForm registrationForm) throws Exception {
        return getCloudStorageModule().uploadRegistrationRequest(registrationForm);
    }
    public boolean acceptRegistrationRequest(RegistrationForm registrationForm) {
        try {
            SecretKey key = safetyModule.generateKey();

            String ADMIN_PASSWORD = safetyModule.getAdminPassword();
            String user_password = registrationForm.getPassword();

            byte[] encrypt_admin_key = safetyModule.encryptKey(key, ADMIN_PASSWORD);
            byte[] encrypt_user_key = safetyModule.encryptKey(key, user_password);

            // Сохраняем зашифрованный ключ локально (пропущенный ранее шаг)
            getLocalStorageModule().createUserFolder(registrationForm.getLogin());
            getLocalStorageModule().saveUserKey(registrationForm.getLogin(), encrypt_user_key);
            getLocalStorageModule().saveAdminKey(registrationForm.getLogin(), encrypt_admin_key);

            Doctor doctor = new Doctor();
            doctor.setLogin(registrationForm.getLogin());
            doctor.setFio(registrationForm.getFullName());
            doctor.setPhone(registrationForm.getPhone());
            doctor.setEmail(registrationForm.getEmail());
            databaseModule.insertDoctor(doctor);

            String login = registrationForm.getLogin();
            getCloudStorageModule().deleteRegistrationRequest(login);

            return true;
        } catch (Exception e) {
            return false;
        }
    }
    public boolean rejectRegistrationRequest(RegistrationForm registrationForm){
        String login = registrationForm.getLogin();
        return getCloudStorageModule().deleteRegistrationRequest(login);
    }
}

