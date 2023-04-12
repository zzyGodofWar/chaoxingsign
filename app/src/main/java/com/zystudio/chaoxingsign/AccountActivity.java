package com.zystudio.chaoxingsign;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;

public class AccountActivity extends AppCompatActivity {
    Button btn_back;
    Button btn_add;
    Button btn_del;
    EditText input_account,input_password;
    TextView lableSelect;
    ArrayAdapter<chaoxing> adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        btn_back = findViewById(R.id.button_back);
        btn_back.setOnClickListener(btn_back_click);

        btn_add = findViewById(R.id.button2);
        btn_add.setOnClickListener(new onClick());

        btn_del = findViewById(R.id.button3);
        btn_del.setOnClickListener(new onClick());

        input_account = findViewById(R.id.editText_account);
        input_password = findViewById(R.id.editText_password);

        lableSelect = findViewById(R.id.textView);

        ListView list = findViewById(R.id.list1);

        adapter = new ArrayAdapter<chaoxing>(AccountActivity.this,android.R.layout.simple_list_item_1,MainActivity.chaoXingAccount);
        list.setAdapter(adapter);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                // TODO Auto-generated method stub
                chooseuser = position;
                showSelect();
            }

        });

    }

    private void showSelect(){
        if(chooseuser == -1){
            lableSelect.setText("未选择");
        }else{
            lableSelect.setText("选择:\n"+MainActivity.chaoXingAccount.get(chooseuser).username);
        }
    }


    private int chooseuser = -1;
    class onClick implements View.OnClickListener{
        public void onClick(View v) {
            chaoxing cx;
            switch (v.getId()) {
                case R.id.button2:
                    chooseuser = -1;
                    showSelect();
                    if(input_account.getText().toString().length() ==0 || input_password.getText().toString().length() == 0){
                        Toast.makeText(getApplicationContext(),"账号和密码都不能为空！",Toast.LENGTH_LONG).show();
                        return;
                    }
                    cx = new chaoxing();
                    StringBuffer errorBuffer = new StringBuffer();
                    boolean loginRet = cx.Login(input_account.getText().toString(),input_password.getText().toString(),errorBuffer);
                    if(loginRet){
                        MainActivity.chaoXingAccount.add(cx);
                        Toast.makeText(getApplicationContext(),"登录成功! 欢迎："+cx.name,Toast.LENGTH_LONG).show();
                        cx.UpdateCourseInfo();
                        boolean savestatus = SaveAccount(cx.uid,cx.username,input_password.getText().toString());
                        if(!savestatus){
                            Toast.makeText(getApplicationContext(),"账号保存失败，但不影响本次使用!",Toast.LENGTH_LONG).show();
                        }
                        input_account.setText("");
                        input_password.setText("");
                    }else {
                        Toast.makeText(getApplicationContext(),"登录失败! "+errorBuffer.toString(),Toast.LENGTH_LONG).show();
                    }
                    adapter.notifyDataSetChanged();
                    break;
                case R.id.button3:
                    if(chooseuser == -1){
                        Toast.makeText(getApplicationContext(),"请选择一个账号",Toast.LENGTH_LONG).show();
                    }else{
                        cx = MainActivity.chaoXingAccount.get(chooseuser);
                        File file = new File(getApplicationContext().getExternalFilesDir(null), cx.uid+".zzy");
                        if(file.exists()){
                            if(file.delete()){
                                Toast.makeText(getApplicationContext(),"账号："+cx.username +" 已从记录中删除！",Toast.LENGTH_LONG).show();
                                MainActivity.chaoXingAccount.remove(chooseuser);
                                adapter.notifyDataSetChanged();
                            }
                        }
                        chooseuser = -1;
                        showSelect();
                    }
                    break;
            }
        }
    }

    public boolean SaveAccount(String uid,String user,String password){
        File file = new File(getApplicationContext().getExternalFilesDir(null), uid+".zzy");
        String data = user + ";" + password;
        try {
            FileOutputStream outStream = new FileOutputStream(file);
            outStream.write(data.getBytes());
            outStream.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    View.OnClickListener btn_back_click = new View.OnClickListener(){
        public void onClick(View v){
            Intent intent = new Intent(AccountActivity.this,MainActivity.class);
            startActivity(intent);
            AccountActivity.this.finish();
        }
    };
}