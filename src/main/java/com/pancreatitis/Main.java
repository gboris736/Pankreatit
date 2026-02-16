package com.pancreatitis;

import com.pancreatitis.modules.diskStorControl.*;

import java.io.IOException;

public class Main {
    static DiskStorageControl diskStorageControl;


    public static void main(){
        diskStorageControl = DiskStorageControl.getInstance();


        try {
            System.out.println(diskStorageControl.getListFilesInPath(diskStorageControl.getAppDir()));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
