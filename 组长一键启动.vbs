' 双击启动：若本脚本无反应，请改用同目录的「组长一键启动.bat」或「组长一键启动.cmd」
Option Explicit
Dim shell, fso, root, batPath

Set shell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")
root = fso.GetParentFolderName(WScript.ScriptFullName)
batPath = root & "\组长一键启动.bat"

If Not fso.FileExists(batPath) Then
    MsgBox "未找到 组长一键启动.bat", vbCritical, "CSRRM"
    WScript.Quit 1
End If

shell.CurrentDirectory = root
' 密码、导入、启动均在 bat 中完成（避免 VBS 与 bat 各问一次密码）
shell.Run """" & batPath & """", 1, False
