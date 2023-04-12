package com.zystudio.chaoxingsign;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;

import android.net.Uri;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.*;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {
    public static List<chaoxing> chaoXingAccount = new ArrayList<chaoxing>();
    private Button btn_signin;
    private Button btn_signin2;
    private Button btn_account;
    private EditText eTxt_console;
    private EditText eTxt_qrcode;
    private TextView label1;
    static boolean loadSetting = false;
    //private chaoxing cx = new chaoxing();
    static int runstate;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_signin = findViewById(R.id.button_signin);
        btn_signin.setOnClickListener(new onClick());

        btn_signin2 = findViewById(R.id.button_signin2);
        btn_signin2.setOnClickListener(new onClick());

        btn_account = findViewById(R.id.button_account);
        btn_account.setOnClickListener(new onClick());

        eTxt_console = findViewById(R.id.editText_console);
        eTxt_console.setMovementMethod(ScrollingMovementMethod.getInstance());

        eTxt_qrcode = findViewById(R.id.editTextQR);

        label1 = findViewById(R.id.label1);


        if(!loadSetting){
            loadSetting = true;
            AddtoConsole("界面加载成功!\t作者：Fizzy");
            runstate = checkForRunState();
            switch (runstate){
                case 0:
                    Toast.makeText(getApplicationContext(),"请检查你的网络状态！",Toast.LENGTH_LONG).show();
                    break;
                case 1:
                    Toast.makeText(getApplicationContext(),"抱歉，软件目前已停止使用！",Toast.LENGTH_LONG).show();
                    break;
                case 2:
                    Toast.makeText(getApplicationContext(),"软件版本太低，请先更新！",Toast.LENGTH_LONG).show();
                    break;
                case 3:
                    Toast.makeText(getApplicationContext(),"有新版本可以用！",Toast.LENGTH_LONG).show();
                    if(UpdateURL != ""){
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("发现新版本可以下载")
                                .setMessage("是否手动前往下载地址更新" + (Updatepassword == ""?"":("\n密码："+Updatepassword)))
                                .setPositiveButton("是", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        OpenURL(UpdateURL);
                                    }
                                })
                                .setNegativeButton("否", null)
                                .show();
                    }
                    break;
            }

            if(runstate>=3){
                haveUpdateLog();
                LoadLocalAccount();
                if(StartToast!=""){
                    Toast.makeText(getApplicationContext(),StartToast,Toast.LENGTH_LONG).show();
                }
            }
        }

        if(runstate>=3){
            if(announcement != "" ){
                AddtoConsole("[公告]\n"+announcement+"\n");
            }
            for(chaoxing cx:chaoXingAccount){
                AddtoConsole("用户登录成功 ("+cx.name + " " + cx.username+")");
            }
        }else if(runstate == 2 && UpdateURL != ""){
            DialogASK();
        }else{
            new Thread(new Runnable() {
                @Override
                public void run() {
                    exit();
                }
            }).start();
        }
    }

    private void haveUpdateLog(){
        boolean need = true;
        File config = new File(getApplicationContext().getExternalFilesDir(null),"UpdateLog.data");
        if(config.exists()){
            StringBuffer fileContent = new StringBuffer();
            try {
                FileInputStream inputStream = new FileInputStream(config);
                int len = 0;
                byte[] buff = new byte[1024];
                while ((len = inputStream.read(buff)) != -1) {
                    fileContent.append(new String(buff, 0, len));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if(Double.parseDouble(fileContent.toString()) != version){
                config.delete();
                String data = String.valueOf(version);
                try {
                    FileOutputStream outStream = new FileOutputStream(config);
                    outStream.write(data.getBytes());
                    outStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }else{
                need = false;
            }
        }else{
            String data = String.valueOf(version);
            try {
                FileOutputStream outStream = new FileOutputStream(config);
                outStream.write(data.getBytes());
                outStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if(need && UpdateLog != ""){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(UpdateLog);
            builder.setPositiveButton("确认",null);
            builder.create().show();
        }
    }

    private void DialogASK(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("版本需要更新");
        builder.setMessage("点击确认手动前往下载地址" + (Updatepassword == ""?"":("\n密码："+Updatepassword)));
        builder.setCancelable(false);

        builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                OpenURL(UpdateURL);
                exit();
            }
        });
        builder.create().show();
    }

    private void OpenURL(String URL){
        Intent intent= new Intent();
        intent.setAction("android.intent.action.VIEW");
        Uri content_url = Uri.parse(URL);
        intent.setData(content_url);
        startActivity(intent);
    }

    static String UpdateURL = new String();
    static String Updatepassword = new String();
    static String UpdateLog = new String();
    static String StartToast = new String();
    static String announcement = new String();
    static double version = 1.4;
    private int checkForRunState(){
        StringBuffer pageTextBuffer = new StringBuffer();
        if(!GetWebPage("https://gitee.com/zzyGodofWar/ownproject/raw/master/chaoxing",pageTextBuffer)){
            return 0;
        }
        String pageText = pageTextBuffer.toString();

        if(pageText.indexOf("[work]")==-1){
            if(!GetWebPage("https://gitee.com/zzyGodofWar/ownproject/blame/master/chaoxing",pageTextBuffer)){
                return 0;
            }
            pageText = pageTextBuffer.toString();
            if(pageText.indexOf("[work]")==-1){
                return 0;
            }
        }

        int pos1 = pageText.indexOf("[work]") + 6;
        int pos2 = pageText.indexOf("[work]",pos1);
        if(!pageText.substring(pos1,pos2).equals("1")){
            return 1;
        }
        pos1 = pageText.indexOf("[lastversion]") + 13;
        pos2 = pageText.indexOf("[lastversion]",pos1);
        double lastversion = Double.parseDouble(pageText.substring(pos1,pos2));
        pos1 = pageText.indexOf("[forceupdate]") + 13;
        pos2 = pageText.indexOf("[forceupdate]",pos1);
        double forceversion = Double.parseDouble(pageText.substring(pos1,pos2));

        pos1 = pageText.indexOf("[updatelog]") + 11;
        pos2 = pageText.indexOf("[updatelog]",pos1);
        if(pos1 < pos2){
            UpdateLog = pageText.substring(pos1,pos2);
        }else{
            UpdateLog = "";
        }

        pos1 = pageText.indexOf("[updateurl]") + 11;
        pos2 = pageText.indexOf("[updateurl]",pos1);
        UpdateURL = pageText.substring(pos1,pos2);
        if(UpdateURL.indexOf("http")!=0){
            UpdateURL = "";
        }

        pos1 = pageText.indexOf("[urlpassword]") + 13;
        pos2 = pageText.indexOf("[urlpassword]",pos1);
        if(pos1 < pos2){
            Updatepassword = pageText.substring(pos1,pos2);
        }else{
            Updatepassword = "";
        }

        if(forceversion > version){
            return 2;
        }
        pos1 = pageText.indexOf("[StartToast]") + 12;
        pos2 = pageText.indexOf("[StartToast]",pos1);
        if(pos1 < pos2){
            StartToast = pageText.substring(pos1,pos2);
        }else{
            StartToast = "";
        }
        pos1 = pageText.indexOf("[announcement]") + 14;
        pos2 = pageText.indexOf("[announcement]",pos1);
        announcement = pageText.substring(pos1,pos2);
        if(pos1 < pos2){
            announcement = pageText.substring(pos1,pos2);
        }else{
            announcement = "";
        }
        if(lastversion > version){
            return 3;
        }
        return 4;
    }

    public static void exit(){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean GetWebPage(String URL,StringBuffer webContent){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(URL);
                    HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("User-Agent","Mozilla/4.0 (compatible; MSIE 9.0; Windows NT 6.1)");
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    connection.setRequestProperty("Referer", URL);
                    connection.setRequestProperty("Accept", "*/*");
                    connection.setRequestProperty("Accept-Language","zh-cn");
                    connection.connect();
                    if(connection.getResponseCode()==200){
                        InputStream inputStream = connection.getInputStream();
                        InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");
                        BufferedReader bufferedReader = new BufferedReader(reader);
                        String temp = null;
                        while ((temp = bufferedReader.readLine()) != null) {
                            webContent.append(temp + "\n");
                        }
                        bufferedReader.close();
                        reader.close();
                        inputStream.close();
                    }
                    connection.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void LoadLocalAccount(){
        //Log.i("Main:",""+getApplicationContext().getExternalFilesDir(null));
        File filelist[] = getApplicationContext().getExternalFilesDir(null).listFiles();
        StringBuffer fileContent = new StringBuffer();
        for(int i=filelist.length-1;i>=0;i--){
            if(filelist[i].toString().indexOf(".zzy")>0){
                fileContent.setLength(0);
                try {
                    FileInputStream inputStream = new FileInputStream(filelist[i]);
                    int len = 0;
                    byte[] buff = new byte[1024];
                    while((len = inputStream.read(buff)) != -1){
                        fileContent.append(new String(buff,0,len));
                    }
                    String userdata[] = fileContent.toString().split(";");
                    if(userdata.length !=2){
                        AddtoConsole("已保存用户登录失败 ("+fileContent.toString()+")");
                        filelist[i].delete();
                    }else{
                        chaoxing cx = new chaoxing();
                        StringBuffer errorBuffer = new StringBuffer();
                        boolean loginRet = cx.Login(userdata[0],userdata[1],errorBuffer);
                        if(loginRet) {
                            chaoXingAccount.add(cx);
                        }else{
                            AddtoConsole("用户："+userdata[0]+" 登录失败 (" + errorBuffer.toString() +")");
                            filelist[i].delete();
                        }
                    }


                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void AddtoConsole(String text){
        eTxt_console.getText().append(text + "\r\n");
        eTxt_console.setSelection(eTxt_console.getText().length(), eTxt_console.getText().length());
    }

    class onClick implements View.OnClickListener {
        public void onClick(View v) {
            chaoxing cx;
            StringBuffer signMsg = new StringBuffer();
            switch (v.getId()) {
                case R.id.button_signin2:
                    if(chaoXingAccount.size() == 0){
                        AddtoConsole("请先添加账号！");
                        break;
                    }
                    if(eTxt_qrcode.getText().toString() == ""){
                        Toast.makeText(getApplicationContext(),"请先填入签到二维码识别后的内容",Toast.LENGTH_LONG).show();
                        break;
                    }
                    String qrtext = eTxt_qrcode.getText().toString();
                    int pos1 = qrtext.indexOf("id=");
                    if(pos1 == -1){
                        Toast.makeText(getApplicationContext(),"内容错误，未包含id信息",Toast.LENGTH_LONG).show();
                        break;
                    }
                    String qrActivityId = qrtext.substring(pos1 + 3,qrtext.indexOf("&",pos1));

//                    pos1 = qrtext.indexOf("c=");
//                    if(pos1 == -1){
//                        Toast.makeText(getApplicationContext(),"内容错误，未包含c信息",Toast.LENGTH_LONG).show();
//                        break;
//                    }
//                    if((pos1 > 0 && qrtext.charAt(pos1 -1) != '&')){
//                        pos1 = qrtext.indexOf("c=",pos1+1);
//                    }
//                    if(pos1 == -1){
//                        Toast.makeText(getApplicationContext(),"内容错误，未包含c信息",Toast.LENGTH_LONG).show();
//                        break;
//                    }
//                    String qrC = qrtext.substring(pos1 + 2,qrtext.indexOf("&",pos1));

                    pos1 = qrtext.indexOf("enc=");
                    if(pos1 == -1){
                        Toast.makeText(getApplicationContext(),"内容错误，未包含enc信息",Toast.LENGTH_LONG).show();
                        break;
                    }
                    String qrEnc = qrtext.substring(pos1 + 4,qrtext.length());


                    //String qrCommit = "&c=" + qrC  + "&enc=" + qrEnc;

                    String qrCommit =  "&enc=" + qrEnc;
                    boolean haveSignActivity = false;
                    for(int i=0;i<chaoXingAccount.size();i++){
                        signMsg.setLength(0);
                        cx = chaoXingAccount.get(i);
                        if(cx.courseInfo.size()==0){
                            cx.UpdateCourseInfo();
                        }
                        if(cx.courseInfo.size()==0){
                            AddtoConsole("用户："+cx.name+" 未获取到课程信息！");
                            continue;
                        }

                        cx.CleanSignActiveList();
                        for(int n=0;n<cx.courseInfo.size();n++){
                            cx.GetSignInActivity(cx.courseInfo.get(n));
                        }

                        for(int n=0;n<cx.qrcodeSign.size();n++){
                            if(cx.qrcodeSign.get(n).activityId.equals(qrActivityId)){
                                haveSignActivity = true;
                                if(cx.SignInActivity(cx.qrcodeSign.get(n),qrCommit,signMsg)){
                                    AddtoConsole("用户："+cx.name + "  课程："+cx.qrcodeSign.get(n).courseInfo.courseName + "  二维码签到成功");
                                }else{
                                    AddtoConsole("用户："+cx.name + "  课程："+cx.qrcodeSign.get(n).courseInfo.courseName + "  二维码签到失败("+ signMsg.toString() +")");
                                }
                                break;
                            }
                        }

                        if(!haveSignActivity){
                            AddtoConsole("当前已登录账号未寻找到二维码对应签到信息");
                            break;
                        }
                    }
                    break;
                case R.id.button_signin:
                    if(chaoXingAccount.size() == 0){
                        AddtoConsole("请先添加账号！");
                        break;
                    }
                    for(int i=0;i<chaoXingAccount.size();i++){
                        cx = chaoXingAccount.get(i);
                        if(cx.courseInfo.size()==0){
                            cx.UpdateCourseInfo();
                        }
                        if(cx.courseInfo.size()==0){
                            AddtoConsole("用户："+cx.name+" 未获取到课程信息！");
                            continue;
                        }
                        cx.CleanSignActiveList();
                        for(int n=0;n<cx.courseInfo.size();n++){
                            cx.GetSignInActivity(cx.courseInfo.get(n));
                        }
                        boolean havesignedtask = false;
                        if(cx.qrcodeSign.size() > 0){
                            havesignedtask = true;
                            AddtoConsole("用户："+cx.name+" 有 " + cx.qrcodeSign.size() + " 个二维码签到");
                            for(int n=0;n<cx.qrcodeSign.size();n++){
                                AddtoConsole("[课程："+cx.qrcodeSign.get(n).courseInfo.courseName + "  Aid:"+cx.qrcodeSign.get(n).activityId + "  "+(n+1)+"/"+cx.qrcodeSign.size()+"]");
                            }
                        }

                        if(cx.codeSign.size()>0){
                            havesignedtask = true;
                            AddtoConsole("用户："+cx.name+" 有 " + cx.codeSign.size() + " 个普通签到");
                            for(int n=cx.codeSign.size()-1,idx = 1;n>=0;n--,idx++){
                                signMsg.setLength(0);
                                if(cx.SignInActivity(cx.codeSign.get(n),signMsg)){
                                    AddtoConsole("[课程：" + cx.codeSign.get(n).courseInfo.courseName + " 签到成功"+ "  "+ idx + "/"+cx.codeSign.size()+"]");

                                }else {
                                    AddtoConsole("[课程：" + cx.codeSign.get(n).courseInfo.courseName + " 签到失败"+ "  "+ idx + "/"+cx.codeSign.size()+"]("+ signMsg.toString() +")");
                                }
                            }
                        }

                        if(!havesignedtask){
                            AddtoConsole("用户："+cx.name+" 没有需要签到的课程");
                        }
                    }
                    break;
                case R.id.button_account:
                    Intent intent = new Intent(MainActivity.this,AccountActivity.class);
                    startActivity(intent);
                    MainActivity.this.finish();
                    break;
            }
        }
    }

}

