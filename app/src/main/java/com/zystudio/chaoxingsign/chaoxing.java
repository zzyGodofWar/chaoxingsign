package com.zystudio.chaoxingsign;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.AccessibleObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class chaoxing {
    public class chaoxingCourseInfo{
        public String courseName;
        public String courseId;
        public String classId;
        public String TeacherName;
        public int RoleType;
    }

    public class chaoxingSignInActivity{
        chaoxingCourseInfo courseInfo;
        String activityId;
    }

    public String uid = "";
    public String username = "";
    public String name = "";
    private String password = "";
    private String Cookies = "";
    private String webpage = "";
    private String courseJson = "";
    public List<chaoxingCourseInfo> courseInfo = new ArrayList<chaoxingCourseInfo>();
    public List<chaoxingSignInActivity> qrcodeSign = new ArrayList<chaoxingSignInActivity>();
    public List<chaoxingSignInActivity> codeSign = new ArrayList<chaoxingSignInActivity>();
    private List<String> signedId = new ArrayList<String>();

    public boolean IsLogined(){
        if(username!=""&&password!=""&&Cookies!=""&&uid!=""){
            return true;
        }
        return false;
    }

    public void Clean(){
        uid = "";
        username = "";
        name = "";
        password = "";
        Cookies = "";
        webpage = "";
        courseInfo.clear();
        signedId.clear();
        qrcodeSign.clear();
        codeSign.clear();
    }

    public void CleanSignActiveList(){
        qrcodeSign.clear();
        codeSign.clear();
    }

    public boolean SignInActivity(chaoxingSignInActivity activity,String additionCommit,StringBuffer signMsg){
        String preSignAddress = "https://mobilelearn.chaoxing.com/newsign/preSign?courseId=课程ID&classId=班级ID&activePrimaryId=活动ID&uid=用户UID";
        //&general=1&sys=1&ls=1&appType=15&isTeacherViewOpen=0&appId=1000";

        preSignAddress = preSignAddress.replaceAll("课程ID",activity.courseInfo.courseId);
        preSignAddress = preSignAddress.replaceAll("班级ID",activity.courseInfo.classId);
        preSignAddress = preSignAddress.replaceAll("活动ID",activity.activityId);
        preSignAddress = preSignAddress.replaceAll("用户UID",uid);

        String finalPreSignAddress = preSignAddress;
        StringBuffer codeBuffer = new StringBuffer();
        StringBuffer signCookies = new StringBuffer();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                StringBuffer buffer = new StringBuffer();
                try {
                    URL url = new URL(finalPreSignAddress);
                    HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("User-Agent","Mozilla/4.0 (compatible; MSIE 9.0; Windows NT 6.1)");
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    connection.setRequestProperty("Referer", finalPreSignAddress);
                    connection.setRequestProperty("Accept", "*/*");
                    connection.setRequestProperty("Accept-Language","zh-cn");
                    connection.setRequestProperty("Cookie",Cookies);
                    connection.connect();

                    String key = null,setCookies = new String();
                    for (int i = 0; (key = connection.getHeaderFieldKey(i)) != null; i++) {
                        if (key.equalsIgnoreCase("set-cookie")) {
                            key = connection.getHeaderField(i);
                            int idx = key.indexOf(";");
                            if(idx != -1){
                                key = key.substring(0,idx);
                            }

                            setCookies += key.trim();
                            setCookies += "; ";
                        }
                    }

                    if(connection.getResponseCode()==200){
                        InputStream inputStream = connection.getInputStream();
                        InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");
                        BufferedReader bufferedReader = new BufferedReader(reader);
                        String temp = null;
                        while ((temp = bufferedReader.readLine()) != null) {
                            buffer.append(temp);
                        }
                        //Log.i("Main:",buffer.toString());

                        int findIdx = -1;
                        int subFindIdx = -1;

                        if(buffer.toString().startsWith("无")){
                            signMsg.append(buffer.toString());
                            codeBuffer.append("false");
                        }else if((findIdx = buffer.indexOf("statuscontent"))!=-1) {
                            subFindIdx = buffer.indexOf("<",findIdx);
                            int contextIdx = buffer.indexOf("签到成功",findIdx);
                            if(contextIdx != -1 && contextIdx < subFindIdx) {
                                codeBuffer.append("true");
                            }
                        }else{
                            signCookies.append(setCookies);

                            int idx1 = -1;
                            int idx2 = -1;

                            idx1 = buffer.indexOf("\"locationText\"");
                            if(idx1 != -1){
                                idx1 = buffer.indexOf("value=\"",idx1) + 7;
                                idx2 = buffer.indexOf("\">",idx1);

                                codeBuffer.append("&address=");
                                codeBuffer.append(buffer.substring(idx1,idx2));
                            }

                            idx1 = buffer.indexOf("\"locationLatitude\"");
                            if(idx1 != -1){
                                idx1 = buffer.indexOf("value=\"",idx1) + 7;
                                idx2 = buffer.indexOf("\">",idx1);

                                codeBuffer.append("&latitude=");
                                codeBuffer.append(buffer.substring(idx1,idx2));
                            }

                            idx1 = buffer.indexOf("\"locationLongitude\"");
                            if(idx1 != -1){
                                idx1 = buffer.indexOf("value=\"",idx1) + 7;
                                idx2 = buffer.indexOf("\">",idx1);

                                codeBuffer.append("&longitude=");
                                codeBuffer.append(buffer.substring(idx1,idx2));
                            }
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
        }
        if(codeBuffer.toString().equals("false")){
            return false;
        }else if(codeBuffer.toString().equals("true")){
            return true;
        }

        String signAddress = "https://mobilelearn.chaoxing.com/pptSign/stuSignajax?activeId=活动ID&uid=用户UID";
        //String scrAddress = "https://mobilelearn.chaoxing.com/v2/apis/sign/signIn?";

        signAddress = signAddress.replaceAll("活动ID",activity.activityId);
        signAddress = signAddress.replaceAll("用户UID",uid);

        if(codeBuffer.length() > 0){
            signAddress += codeBuffer.toString();
        }


        if(additionCommit!=""){
            signAddress += additionCommit;
        }

        Log.i("SIGNActivity",signAddress);


        codeBuffer.setLength(0);
        String finalSignAddress = signAddress;
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                StringBuffer buffer = new StringBuffer();
                try {
                    URL url = new URL(finalSignAddress);
                    HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("User-Agent","Mozilla/4.0 (compatible; MSIE 9.0; Windows NT 6.1)");
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    connection.setRequestProperty("Referer", finalPreSignAddress);
                    connection.setRequestProperty("Accept", "*/*");
                    connection.setRequestProperty("Accept-Language","zh-cn");
                    connection.setRequestProperty("Cookie", Cookies + signCookies.toString());
                    connection.connect();
                    if(connection.getResponseCode()==200){
                        InputStream inputStream = connection.getInputStream();
                        InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");
                        BufferedReader bufferedReader = new BufferedReader(reader);
                        String temp = null;
                        while ((temp = bufferedReader.readLine()) != null) {
                            buffer.append(temp);
                        }
                        //Log.i("Main:",buffer.toString());
                        signMsg.append(buffer.toString());
                        if(buffer.toString().indexOf("success")!= -1 || buffer.toString().indexOf("已签到")!= -1){
                            codeBuffer.append("true");
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
        }
        if(codeBuffer.toString().equals("true")){
            signedId.add(activity.activityId);
            return true;
        }else{
            return false;
        }

    }

    public boolean SignInActivity(chaoxingSignInActivity info,StringBuffer signMsg){
        return SignInActivity(info,"",signMsg);
    }


    public String GetName(){
        if(name=="" && IsLogined()){
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try{
                        URL url = new URL("http://i.chaoxing.com/base");
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestProperty("User-Agent","Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.186 Safari/537.36");
                        connection.setRequestProperty("Cookie",Cookies);
                        if (connection.getResponseCode() == 200) {
                            InputStream inputStream = connection.getInputStream();
                            InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");
                            BufferedReader bufferedReader = new BufferedReader(reader);
                            StringBuffer buffer = new StringBuffer();
                            String temp = null;

                            while ((temp = bufferedReader.readLine()) != null) {
                                buffer.append(temp);
                            }
                            bufferedReader.close();
                            reader.close();
                            inputStream.close();
                            temp = buffer.toString();
                            int pos = temp.indexOf("<p class=\"user-name\">");


                            if(pos!=-1){
                                pos+="<p class=\"user-name\">".length();
                                name = temp.substring(pos, temp.indexOf("</p>",pos+1));
                            }
                        }
                    }catch (Exception e) {
                        e.printStackTrace();
                        name = null;
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
        return name;
    }

    private boolean IsActivityFinish(String aid){
        if(-1 == signedId.lastIndexOf(aid)){
            return false;
        }
        return true;
    }

    public void GetSignInActivity(chaoxingCourseInfo cinfo){
        if(!IsLogined()){
            return;
        }
        String getUrl = "https://mobilelearn.chaoxing.com/widget/pcpick/stu/activeList?courseId=课程ID&classId=班级ID&puid=" + uid;
        getUrl = getUrl.replaceAll("课程ID",cinfo.courseId);
        getUrl = getUrl.replaceAll("班级ID",cinfo.classId);
        String finalGetUrl = getUrl;
        StringBuffer buffer = new StringBuffer();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    URL url = new URL(finalGetUrl);
                    HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                    connection.setRequestProperty("Cookie",Cookies);
                    if (connection.getResponseCode() == 200) {
                        InputStream inputStream = connection.getInputStream();
                        InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");
                        BufferedReader bufferedReader = new BufferedReader(reader);
                        String temp = null;
                        while ((temp = bufferedReader.readLine()) != null) {
                            buffer.append(temp);
                        }
                        bufferedReader.close();
                        reader.close();
                        inputStream.close();
                    }
                }catch (Exception e) {
                    e.printStackTrace();
                    buffer.delete(0,buffer.length());
                }
            }
        });
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            JSONObject json = new JSONObject(buffer.toString());
            if(1!=json.getInt("result")){
                return;
            }
            JSONArray jsonArray = json.getJSONObject("data").getJSONArray("startList");
            for(int i=0;i<jsonArray.length();i++){
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                if(jsonObject.getInt("activeType")==2 && jsonObject.getInt("status")==1){
                    String activityId = jsonObject.getString("id");
                    if(signedId.indexOf(activityId)!=-1){
                        continue;
                    }
                    chaoxingSignInActivity Aid = new chaoxingSignInActivity();
                    Aid.courseInfo = cinfo;
                    Aid.activityId = activityId;
                    if(jsonObject.getInt("otherId")==2){
                        qrcodeSign.add(Aid);
                    }else{
                        codeSign.add(Aid);
                    }
                }
            }
        }catch (JSONException e){
            e.printStackTrace();
        }
        return;
    }

    public String toString(){
        return "姓名:"+name+" 账号："+username;
    }

    public boolean UpdateCourseInfo(){
        if(!IsLogined()){
            return false;
        }

        Thread thread = new Thread(new Runnable() {
            StringBuffer buffer = new StringBuffer();
            public void run() {
                try{
                    URL url = new URL("http://mooc1-api.chaoxing.com/mycourse/backclazzdata?view=json&rss=1");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestProperty("Cookie",Cookies);
                    if (connection.getResponseCode() == 200) {
                        InputStream inputStream = connection.getInputStream();
                        InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");
                        BufferedReader bufferedReader = new BufferedReader(reader);
                        String temp = null;

                        while ((temp = bufferedReader.readLine()) != null) {
                            buffer.append(temp);
                        }
                        bufferedReader.close();
                        reader.close();
                        inputStream.close();

                        courseJson = buffer.toString();
                    }
                }catch (Exception e) {
                    e.printStackTrace();
                    courseJson = "";
                }
            }
        });
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try{
            JSONObject json = new JSONObject(courseJson);
            JSONArray jsonArray = json.getJSONArray("channelList");
            for(int i=0;i<jsonArray.length();i++){
                JSONObject jcontent = jsonArray.getJSONObject(i).getJSONObject("content");
                if(!jcontent.has("course")){
                    continue;
                }
                //Log.i("Main:",jsonArray.getJSONObject(i).getJSONObject("content").getJSONObject("course").getJSONArray("data").getJSONObject(0).getString("name"));
                chaoxingCourseInfo tmpInfo = new chaoxingCourseInfo();
                tmpInfo.courseName = jcontent.getJSONObject("course").getJSONArray("data").getJSONObject(0).getString("name");
                tmpInfo.courseId = jcontent.getJSONObject("course").getJSONArray("data").getJSONObject(0).getString("id");
                tmpInfo.classId = jcontent.getString("id");
                tmpInfo.TeacherName = jcontent.getJSONObject("course").getJSONArray("data").getJSONObject(0).getString("teacherfactor");
                tmpInfo.RoleType = jcontent.getInt("id");
                if(tmpInfo.courseName!=null && tmpInfo.courseName !="" && tmpInfo.classId != null && tmpInfo.classId != ""){
                    courseInfo.add(tmpInfo);
                }
            }
        }catch (JSONException e){
            e.printStackTrace();
            Log.i("Main:",this.name + " 课程JSON错误 "+e.getMessage());
        }
        return true;
    }

    public boolean Login(String username, String password, StringBuffer errorString) {
        this.username = username;
        this.password = password;

        String loginUrl = "http://passport2.chaoxing.com/fanyalogin?uname=账号&password=密码";
        loginUrl = loginUrl.replaceAll("账号", username);
        loginUrl = loginUrl.replaceAll("密码", password);
        String finallyLoginUrl = loginUrl;
        boolean ret = false;
        webpage = "";

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                StringBuffer buffer = new StringBuffer();
                try {
                    URL url = new URL(finallyLoginUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("User-Agent","Mozilla/4.0 (compatible; MSIE 9.0; Windows NT 6.1)");
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    connection.setRequestProperty("Referer", finallyLoginUrl);
                    connection.setRequestProperty("Accept", "*/*");
                    connection.setRequestProperty("Accept-Language","zh-cn");
                    connection.connect();
                    String key = null,setCookies = new String();
                    for (int i = 0; (key = connection.getHeaderFieldKey(i)) != null; i++) {
                        if (key.equalsIgnoreCase("set-cookie")) {
                            key = connection.getHeaderField(i);
                            int idx = key.indexOf(";");
                            if(idx != -1){
                                key = key.substring(0,idx);
                            }

                            setCookies += key.trim();
                            setCookies += "; ";
                        }
                    }
                    if (connection.getResponseCode() == 200) {
                        InputStream inputStream = connection.getInputStream();
                        InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");
                        BufferedReader bufferedReader = new BufferedReader(reader);
                        String temp = null;

                        while ((temp = bufferedReader.readLine()) != null) {
                            buffer.append(temp);
                        }
                        bufferedReader.close();
                        reader.close();
                        inputStream.close();
                        //Log.i("Main:", buffer.toString());
                        //Log.i("cookies",setCookies);
                        Cookies = setCookies;
                    }
                    connection.disconnect();
                    webpage = buffer.toString();
                } catch (Exception e) {
                    //Log.e("Main:", "HTTP Error");
                    e.printStackTrace();
                    webpage = "网络访问错误";
                }
            }
        });
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            errorString.append("等待线程错误");
            e.printStackTrace();
        }

        if(webpage!=""){
            try{
                JSONObject json = new JSONObject(webpage);
                //Log.i("Main:",json.getString("status"));
                if(json.getString("status")=="true"){
                    int pos = Cookies.indexOf("_uid=");
                    if(pos!=-1){
                        pos += "_uid=".length();
                        uid = Cookies.substring(pos, Cookies.indexOf(";",pos+1));
                        GetName();
                        ret = true;
                    }else{
                        errorString.append(webpage);
                        Clean();
                    }
                }else{
                    errorString.append(json.getString("msg2"));
                }
            } catch (JSONException e) {
                //Log.i("Main:","Json failure");
                errorString.append("JSON解析错误");
                e.printStackTrace();
                return false;
            }
        }
        //Log.i("Main:","quit");
        return ret;
    }

   /* private String LoginThread(){

    }*/

}
