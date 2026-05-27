' 双击本文件：弹出对话框输入密码，无需手动打开 PowerShell
' 内部会调用同目录下的 组长一键启动.bat

Option Explicit
Dim shell, fso, root, batPath, pwd, ans, reimport, pwdFile, importFile

Set shell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")
root = fso.GetParentFolderName(WScript.ScriptFullName)
batPath = root & "\组长一键启动.bat"

If Not fso.FileExists(batPath) Then
    MsgBox "未找到 组长一键启动.bat ，请确认从 Git 完整下载项目。", vbCritical, "CSRRM"
    WScript.Quit 1
End If

If Not fso.FileExists(root & "\pom.xml") Then
    MsgBox "请在含 pom.xml 的项目根目录运行本启动器。", vbCritical, "CSRRM"
    WScript.Quit 1
End If

pwd = InputBox("请输入你本机 MySQL 的 root 密码：" & vbCrLf & vbCrLf & _
    "（仅保存在本机，不会上传到 GitHub）", "校园自习室系统 - 一键启动", "")
If pwd = "" Then
    MsgBox "已取消。", vbInformation, "CSRRM"
    WScript.Quit 0
End If

ans = MsgBox("是否导入数据库快照？" & vbCrLf & vbCrLf & _
    "首次使用：请点「是」" & vbCrLf & _
    "以后日常启动：可点「否」加快速度", vbYesNoCancel + vbQuestion, "导入 database-full.sql")
If ans = vbCancel Then WScript.Quit 0
If ans = vbYes Then
    reimport = "Y"
Else
    reimport = "N"
End If

pwdFile = shell.ExpandEnvironmentStrings("%TEMP%") & "\csrrm_mysql_pwd.txt"
importFile = shell.ExpandEnvironmentStrings("%TEMP%") & "\csrrm_do_import.txt"

Dim tf
Set tf = fso.CreateTextFile(pwdFile, True, False)
tf.Write pwd
tf.Close

Set tf = fso.CreateTextFile(importFile, True, False)
tf.Write reimport
tf.Close

' 显示命令行窗口，便于查看进度与报错（不强制用户自己打开 PowerShell）
shell.CurrentDirectory = root
shell.Run "cmd /c """ & batPath & """ auto", 1, False
