# General Symbol Definitions
!define PROD_NAME "BibAnt"
!define REGKEY "SOFTWARE\LeanPulse\${PROD_NAME}"
!define COMPANY LeanPulse
!define URL http://www.leanpulse.com/
!ifndef VERSION
	!define VERSION "1.0.0"
!endif
!ifndef CUSTOMER
	!define CUSTOMER "LeanPulse"
!endif

!define JRE_URL "http://download.leanpulse.com/JRE_setup.exe"
!define PDFCREATOR_URL "http://download.leanpulse.com/PDFCreator_setup.exe"

# Installer attributes
Name "${PROD_NAME}"
Caption "${COMPANY} ${PROD_NAME} Setup"
OutFile "..\#dist\${PROD_NAME}_${VERSION}_${CUSTOMER}.exe"
InstType "Full"
InstType "Minimal"
InstallDir "${PROD_NAME}"
ShowInstDetails show
CRCCheck on
SetCompressor /SOLID lzma
VIProductVersion 1.0.0.0
VIAddVersionKey ProductName "${PROD_NAME}"
VIAddVersionKey ProductVersion "${VERSION}"
VIAddVersionKey CompanyName "${COMPANY}"
VIAddVersionKey CompanyWebsite "${URL}"
VIAddVersionKey FileVersion "${VERSION}"
VIAddVersionKey FileDescription "${PROD_NAME} v${VERSION}"
VIAddVersionKey LegalCopyright "© ${COMPANY}"
InstallDirRegKey HKLM "${REGKEY}" Path
ShowUninstDetails show

# MultiUser Symbol Definitions
!define MULTIUSER_EXECUTIONLEVEL Admin
!define MULTIUSER_INSTALLMODE_DEFAULT_REGISTRY_KEY "${REGKEY}"
!define MULTIUSER_INSTALLMODE_DEFAULT_REGISTRY_VALUENAME MultiUserInstallMode
!define MULTIUSER_INSTALLMODE_INSTDIR "${PROD_NAME}"
!define MULTIUSER_INSTALLMODE_INSTDIR_REGISTRY_KEY "${REGKEY}"
!define MULTIUSER_INSTALLMODE_INSTDIR_REGISTRY_VALUE "Path"

# MUI Symbol Definitions
!define MUI_ABORTWARNING
!define MUI_ICON "${NSISDIR}\Contrib\Graphics\Icons\orange-install.ico"
!define MUI_HEADERIMAGE
!define MUI_HEADERIMAGE_BITMAP "${NSISDIR}\Contrib\Graphics\Header\orange.bmp"
!define MUI_WELCOMEFINISHPAGE_BITMAP "${NSISDIR}\Contrib\Graphics\Wizard\orange.bmp"
!define MUI_COMPONENTSPAGE_SMALLDESC
!define MUI_FINISHPAGE_SHOWREADME $INSTDIR\manual\index.html
!define MUI_FINISHPAGE_SHOWREADME_TEXT "Open the BibAnt manual."
!define MUI_FINISHPAGE_NOREBOOTSUPPORT
!define MUI_UNICON "${NSISDIR}\Contrib\Graphics\Icons\orange-uninstall.ico"

# Included files
!include MultiUser.nsh
!include Sections.nsh
!include MUI.nsh
!include EnvVarUpdate.nsh

# Installer pages
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_LICENSE ..\#dist\app\license.txt
!insertmacro MUI_PAGE_COMPONENTS
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

# Installer languages
!insertmacro MUI_LANGUAGE English


# Installer sections
Section "Check and Install Prerequisites" SECPREREQUISITES
	SectionIn 1
	
	;Skip the prerequisites check if silent install
	IfSilent DonePDFCreator
	
	DetailPrint "Checking Bibant prerequisites..."
	
	;Check for compatible JRE (>= 1.5)
	DetailPrint "Looking for a compatible Java Runtime Environment (>= 1.5)..."
	ReadRegStr $R0 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" CurrentVersion
    StrCmp "" "$R0" DownloadJava CheckJavaVer
	CheckJavaVer:
		ReadRegStr $0 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$R0" JavaHome
        GetFullPathName /SHORT $R1 "$0"
		StrCpy $0 $R0 1 0
		StrCpy $1 $R0 1 2
		StrCpy $R0 "$0$1"
		IntCmp 15 $R0 FoundCorrectJavaVer FoundCorrectJavaVer DownloadJava
	FoundCorrectJavaVer:
		IfFileExists "$R1\bin\javaw.exe" 0 DownloadJava
		DetailPrint "OK: Compatible JRE found."
		Goto DoneJava
	DownloadJava:
		DetailPrint "No compatible JRE found, downloading the JRE setup..."
		NSISdl::download /TIMEOUT=10000 ${JRE_URL} "$TEMP\JRE_setup.exe"
		Pop $0 ;Get the return value
		StrCmp $0 "success" InstallJRE 0
		StrCmp $0 "cancel" 0 +3
		DetailPrint "ERROR: Download cancelled!"
		Goto ErrorJavaDownload
		DetailPrint "ERROR: Unkown error during download!"
		Goto ErrorJavaDownload
	ErrorJavaDownload:
		MessageBox MB_OK "An error occured while trying to install a compatible Java Runtime Environment (>= 1.5). Please visit http://www.java.com/download/ to manually download and install the JRE."
		Goto DoneJava
	InstallJRE:
		DetailPrint "Download succeed, launching JRE setup..."
		ExecWait "$TEMP\JRE_setup.exe" $0
		Delete "$TEMP\JRE_setup.exe"
		StrCmp $0 "0" +3 0
		DetailPrint "ERROR: An error occured while installing the JRE!"
		Goto ErrorJavaDownload
		DetailPrint "OK: Setup finished."
	DoneJava:
	
	;Check for a compatible installation of PDFCreator (>= 1.0)
	DetailPrint "Looking for a compatible installation of PDFCreator (>= 1.0)..."
	ReadRegStr $R0 HKLM "SOFTWARE\PDFCreator\Program" ApplicationVersion
    StrCmp "" "$R0" DownloadPDFCreator CheckPDFCreatorVer
	CheckPDFCreatorVer:
		StrCpy $0 $R0 1 0
		StrCpy $1 $R0 1 2
		StrCpy $R0 "$0$1"
		IntCmp 10 $R0 FoundCorrectPDFCreatorVer FoundCorrectPDFCreatorVer DownloadPDFCreator
	FoundCorrectPDFCreatorVer:
		DetailPrint "OK: Compatible PDFCreator found."
		Goto DonePDFCreator
	DownloadPDFCreator:
		DetailPrint "No compatible installation of PDFCreator found, downloading the PDFCreator setup..."
		NSISdl::download /TIMEOUT=10000 ${PDFCREATOR_URL} "$TEMP\PDFCreator_setup.exe"
		Pop $0 ;Get the return value
		StrCmp $0 "success" InstallPDFCreator 0
		StrCmp $0 "cancel" 0 +3
		DetailPrint "ERROR: Download cancelled!"
		Goto ErrorPDFCreatorDownload
		DetailPrint "ERROR: Unkown error during download!"
		Goto ErrorPDFCreatorDownload
	ErrorPDFCreatorDownload:
		MessageBox MB_OK "An error occured while trying to install PDFCreator. Please visit http://sourceforge.net/projects/pdfcreator/ to manually download and install PDFCreator."
		Goto DonePDFCreator
	InstallPDFCreator:
		DetailPrint "Download succeed, launching PDFCreator setup..."
		ExecWait '"$TEMP\PDFCreator_setup.exe" /SP- /SILENT /NOCANCEL /NORESTART /NOICONS /COMPONENTS="program,ghostscript" /TASKS=""' $0
		Delete "$TEMP\PDFCreator_setup.exe"
		StrCmp $0 "0" +3 0
		DetailPrint "ERROR: An error occured while installing PDFCreator!"
		Goto ErrorPDFCreatorDownload
		DetailPrint "OK: Setup finished."
	DonePDFCreator:
	
SectionEnd


Section "${PROD_NAME} Core Files (required)" SECCORE
	SectionIn RO
	SetDetailsPrint both
	DetailPrint "Installing ${PROD_NAME} Core Files..."
	SetDetailsPrint listonly
	
	SetOutPath $INSTDIR
	SetOverwrite on
	File /r ..\#dist\app\*.*
SectionEnd


Section "Start Menu Shortcuts" SECSHORTCUTS
	SectionIn 1
	CreateDirectory "$SMPROGRAMS\${PROD_NAME}"
	CreateShortcut "$SMPROGRAMS\${PROD_NAME}\Execute ${PROD_NAME}.lnk" cmd "/k bibant"
	CreateShortcut "$SMPROGRAMS\${PROD_NAME}\${PROD_NAME} Manual.lnk" $INSTDIR\manual\index.html
	CreateShortcut "$SMPROGRAMS\${PROD_NAME}\Uninstall ${PROD_NAME}.lnk" $INSTDIR\uninstall.exe
	WriteRegStr HKLM "${REGKEY}\Components" Shortcuts 1
SectionEnd


Section "Update the System Path" SECSYSPATH
	SectionIn 1
	${EnvVarUpdate} $0 "PATH" "A" "HKLM" $INSTDIR
	WriteRegStr HKLM "${REGKEY}\Components" SysPath 1
SectionEnd


Section -post
	SetDetailsPrint both
	DetailPrint "Creating Registry Keys..."
	SetDetailsPrint listonly
	
	WriteRegStr HKLM "${REGKEY}" Path $INSTDIR
	WriteUninstaller $INSTDIR\uninstall.exe
	WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${PROD_NAME}" DisplayName "${COMPANY} ${PROD_NAME} v${VERSION}"
	WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${PROD_NAME}" DisplayVersion "${VERSION}"
	WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${PROD_NAME}" Publisher "${COMPANY}"
	WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${PROD_NAME}" URLInfoAbout "${URL}"
	WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${PROD_NAME}" DisplayIcon $INSTDIR\uninstall.exe
	WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${PROD_NAME}" UninstallString $INSTDIR\uninstall.exe
	WriteRegDWORD HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${PROD_NAME}" NoModify 1
	WriteRegDWORD HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${PROD_NAME}" NoRepair 1
	
	SetOutPath "$DOCUMENTS"
SectionEnd


# Uninstaller sections
Section un.Core UNSECCORE
	RmDir /r /REBOOTOK $INSTDIR
SectionEnd


Section /o un.Shortcuts UNSECSHORTCUTS
	Delete /REBOOTOK "$SMPROGRAMS\${PROD_NAME}\Execute ${PROD_NAME}.lnk"
	Delete /REBOOTOK "$SMPROGRAMS\${PROD_NAME}\${PROD_NAME} Manual.lnk"
	Delete /REBOOTOK "$SMPROGRAMS\${PROD_NAME}\Uninstall ${PROD_NAME}.lnk"
	RmDir /REBOOTOK $SMPROGRAMS\${PROD_NAME}
	DeleteRegValue HKLM "${REGKEY}\Components" Shortcuts
SectionEnd


Section /o un.SysPath UNSECSYSPATH
	${un.EnvVarUpdate} $0 "PATH" "R" "HKLM" $INSTDIR
	DeleteRegValue HKLM "${REGKEY}\Components" SysPath
SectionEnd


Section -un.post
	DeleteRegKey HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${PROD_NAME}"
	Delete /REBOOTOK $INSTDIR\uninstall.exe
	DeleteRegValue HKLM "${REGKEY}" Path
	DeleteRegKey /IfEmpty HKLM "${REGKEY}\Components"
	DeleteRegKey /IfEmpty HKLM "${REGKEY}"
	RmDir /r /REBOOTOK $INSTDIR
SectionEnd


# Macro for selecting uninstaller sections
!macro SELECT_UNSECTION SECTION_NAME UNSECTION_ID
	Push $R0
	ReadRegStr $R0 HKLM "${REGKEY}\Components" "${SECTION_NAME}"
	StrCmp $R0 1 0 next${UNSECTION_ID}
	!insertmacro SelectSection "${UNSECTION_ID}"
	GoTo done${UNSECTION_ID}
next${UNSECTION_ID}:
	!insertmacro UnselectSection "${UNSECTION_ID}"
done${UNSECTION_ID}:
	Pop $R0
!macroend


# Installer functions
Function .onInit
	InitPluginsDir
	!insertmacro MULTIUSER_INIT
FunctionEnd


# Uninstaller functions
Function un.onInit
	SetAutoClose false
	!insertmacro MULTIUSER_UNINIT
	!insertmacro SELECT_UNSECTION Shortcuts ${UNSECSHORTCUTS}
	!insertmacro SELECT_UNSECTION SysPath ${UNSECSYSPATH}
FunctionEnd


# Section Descriptions
!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
	!insertmacro MUI_DESCRIPTION_TEXT ${SECPREREQUISITES} "Check ${PROD_NAME} prerequisites, download and install them if not already installed."
	!insertmacro MUI_DESCRIPTION_TEXT ${SECCORE} "The core files required to use ${PROD_NAME}."
	!insertmacro MUI_DESCRIPTION_TEXT ${SECSHORTCUTS} "Create program's shortcuts in the Start Menu."
	!insertmacro MUI_DESCRIPTION_TEXT ${SECSYSPATH} "Add ${PROD_NAME} installation directory to the system PATH."
!insertmacro MUI_FUNCTION_DESCRIPTION_END
