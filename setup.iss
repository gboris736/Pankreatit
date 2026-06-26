;===========================================================
; Скрипт Inno Setup для PancreatitManager
; Собран на основе образа jpackage (app-image)
;===========================================================

#define AppName "PancreatitManager"
#define AppVersion "1.1.6"
#define AppPublisher "Pancreatitis Team"
#define AppExeName "PancreatitManager.exe"
#define AppImageDir "D:\Projects\Pankreatit\target\app_image\PancreatitManager"

[Setup]
AppId={{8E2B8D4A-9F3C-4E5A-9B7C-6D1E8F2A4B3C}
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher={#AppPublisher}
DefaultDirName={pf}\{#AppName}
DefaultGroupName={#AppName}
UninstallDisplayIcon={app}\{#AppExeName}
OutputDir=installer
OutputBaseFilename=PancreatitManagerSetup
Compression=lzma2
SolidCompression=yes
ArchitecturesInstallIn64BitMode=x64
PrivilegesRequired=admin
; SetupIconFile={#AppImageDir}\{#AppExeName}   ; использовать иконку из приложения (если есть)

[Files]
; Копируем все файлы образа рекурсивно
Source: "{#AppImageDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
; Ярлык в меню Пуск (для всех пользователей)
Name: "{commonprograms}\{#AppName}\{#AppName}"; Filename: "{app}\{#AppExeName}"; WorkingDir: "{app}"
; Ярлык на рабочем столе (для всех пользователей)
Name: "{commondesktop}\{#AppName}"; Filename: "{app}\{#AppExeName}"; WorkingDir: "{app}"

[Registry]
; Добавляем запись в список установленных программ (Add/Remove Programs)
; Root: HKLM; Subkey: "Software\Microsoft\Windows\CurrentVersion\Uninstall\{#AppName}"; Flags: uninsdeletekey
; Root: HKLM; Subkey: "Software\Microsoft\Windows\CurrentVersion\Uninstall\{#AppName}"; ValueType: string; ValueName: "DisplayName"; ValueData: "{#AppName}"
; Root: HKLM; Subkey: "Software\Microsoft\Windows\CurrentVersion\Uninstall\{#AppName}"; ValueType: string; ValueName: "DisplayVersion"; ValueData: "{#AppVersion}"
; Root: HKLM; Subkey: "Software\Microsoft\Windows\CurrentVersion\Uninstall\{#AppName}"; ValueType: string; ValueName: "Publisher"; ValueData: "{#AppPublisher}"
; Root: HKLM; Subkey: "Software\Microsoft\Windows\CurrentVersion\Uninstall\{#AppName}"; ValueType: string; ValueName: "DisplayIcon"; ValueData: "{app}\{#AppExeName}"
; Root: HKLM; Subkey: "Software\Microsoft\Windows\CurrentVersion\Uninstall\{#AppName}"; ValueType: string; ValueName: "UninstallString"; ValueData: "{uninstallexe}"

[Run]
; Опциональный запуск приложения после установки
Filename: "{app}\{#AppExeName}"; Description: "Запустить {#AppName}"; Flags: postinstall nowait skipifsilent

[Code]
// Функция, вызываемая перед началом деинсталляции
// Предлагает пользователю удалить личные данные (папка в Roaming)
function InitializeUninstall(): Boolean;
var
  DeleteData: Integer;
  RoamingFolder: string;
begin
  RoamingFolder := ExpandConstant('{userappdata}\{#AppName}');
  if DirExists(RoamingFolder) then
  begin
    DeleteData := MsgBox('Удалить личные данные приложения?' + #13#10 +
                         'Нажмите "Да" – удалить, "Нет" – оставить.',
                         mbConfirmation, MB_YESNO);
    if DeleteData = IDYES then
    begin
      // Рекурсивно удаляем папку со всем содержимым
      DelTree(RoamingFolder, True, True, True);
    end;
  end;
  Result := True;
end;