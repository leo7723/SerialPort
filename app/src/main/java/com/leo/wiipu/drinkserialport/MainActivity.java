package com.leo.wiipu.drinkserialport;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.IdRes;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import cn.wch.ch34xuartdriver.CH34xUARTDriver;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private static final String ACTION_USB_PERMISSION = "com.leo.wiipu.drinkserialport.USB_PERMISSION";
    public byte[] writeBuffer;
    public byte[] readBuffer;
    public boolean isOpen=false;

    public ReadThread readThread;
    private MyHandler handler;
    private int retval;
    public int baudRate=9600;//波特率

    byte databit=8;
    byte stopBit=1;
    byte parity=0;
    byte flowControl=0;

    private Switch aSwitch;
    private RadioGroup radioGroup;
    private RadioButton minBtn,normolBtn,maxBtn;
    private EditText editText;
    private Button btnWrite,btnClear,btnRead;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MainApplicanton.driver = new CH34xUARTDriver(
                (UsbManager) getSystemService(Context.USB_SERVICE), this,
                ACTION_USB_PERMISSION);//初始化驱动入口

//        MainApplicanton.driver.SetConfig(baudRate, databit, stopBit, parity, //配置串口波特率，函数说明可参照编程手册
//                flowControl);

        initView();
        if (!MainApplicanton.driver.UsbFeatureSupported())// 判断系统是否支持USB HOST ***
        {
            Dialog dialog = new AlertDialog.Builder(MainActivity.this)
                    .setTitle("提示")
                    .setMessage("您的手机不支持USB HOST，请更换其他手机再试！")
                    .setPositiveButton("确认",
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface arg0,
                                                    int arg1) {
                                    finish();
                                }
                            }).create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);// 保持常亮的屏幕的状态

        writeBuffer = new byte[512];
        readBuffer = new byte[512];
        isOpen=false;
        handler=new MyHandler(MainActivity.this);

    }

    private void initView(){
        aSwitch= (Switch) findViewById(R.id.aswitch);
        radioGroup= (RadioGroup) findViewById(R.id.radio_group);
        minBtn= (RadioButton) findViewById(R.id.btl_9600);
        normolBtn= (RadioButton) findViewById(R.id.btl_19200);
        maxBtn= (RadioButton) findViewById(R.id.btl_19200);
        editText= (EditText) findViewById(R.id.editText);
        btnWrite= (Button) findViewById(R.id.btn_send);
        btnClear= (Button) findViewById(R.id.btn_send);
        textView= (TextView) findViewById(R.id.write_text);
        btnRead= (Button) findViewById(R.id.btn_read);

        btnWrite.setOnClickListener(this);
        btnClear.setOnClickListener(this);
        btnRead.setOnClickListener(this);

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, @IdRes int i) {
                switch (radioGroup.getCheckedRadioButtonId()){
                    case R.id.btl_9600: baudRate=9600;break;
                    case R.id.btl_19200: baudRate=19200;break;
                    case R.id.btl_115200: baudRate=115200;break;
                }
            }
        });

        aSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Toast.makeText(MainActivity.this, "开始初始化",
                        Toast.LENGTH_SHORT).show();
                if (!isOpen) {

                    retval = MainApplicanton.driver.ResumeUsbList();
                    if (retval == -1)// ResumeUsbList方法用于枚举CH34X设备以及打开相关设备
                    {
                        Toast.makeText(MainActivity.this, "打开设备失败!",
                                Toast.LENGTH_SHORT).show();
                        MainApplicanton.driver.CloseDevice();
                        aSwitch.setChecked(false);
                    } else if (retval == 0) {

                        Toast.makeText(MainActivity.this, "开始初始化",
                                Toast.LENGTH_SHORT).show();
                        if (!MainApplicanton.driver.UartInit()) {//对串口设备进行初始化操作
                            Toast.makeText(MainActivity.this, "设备初始化失败!",
                                    Toast.LENGTH_SHORT).show();
                            Toast.makeText(MainActivity.this, "打开" +
                                            "设备失败!",
                                    Toast.LENGTH_SHORT).show();
                            aSwitch.setChecked(false);
                            return;
                        }
                        Toast.makeText(MainActivity.this, "打开设备成功!",
                                Toast.LENGTH_SHORT).show();
                        isOpen = true;
                        btnWrite.setEnabled(true);
                        readThread=new ReadThread();//开启读线程读取串口接收的数据
//                        readThread.run();
                    }else {
                        Dialog dialog=new AlertDialog.Builder(MainActivity.this)
                                .setIcon(R.mipmap.ic_launcher)
                                .setTitle("未授权限")
                                .setMessage("确认退出吗？")
                                .setPositiveButton("确定", new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        finish();
                                    }
                                })
                                .setNegativeButton("返回", new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // TODO Auto-generated method stub
                                        dialog.dismiss();
                                    }
                                })
                                .create();
                        dialog.show();

                    }
                }else {
                    MainApplicanton.driver.CloseDevice();
                    btnWrite.setEnabled(false);
                    isOpen = false;
                }
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_clear :
                textView.setText("waiting ...");
                break;
            case R.id.btn_send:
                startWrite();
                break;
            case R.id.btn_read:
                startRead();
                break;

        }
    }
    private void startWrite(){

        if (MainApplicanton.driver.SetConfig(baudRate, databit, stopBit, parity, //配置串口波特率，函数说明可参照编程手册
                flowControl)) {
//            Toast.makeText(MainActivity.this, "串口设置成功!",
//                    Toast.LENGTH_SHORT).show();
            byte[] to_send = toByteArray(editText.getText().toString());
//				byte[] to_send = toByteArray2(writeText.getText().toString());
            int retval = MainApplicanton.driver.WriteData(to_send, to_send.length);//写数据，第一个参数为需要发送的字节数组，第二个参数为需要发送的字节长度，返回实际发送的字节长度
            if (retval < 0)
                Toast.makeText(MainActivity.this, "写失败!",
                        Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(MainActivity.this, "串口设置失败!",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void startRead(){
        byte[] buffer = new byte[4096];
//        Message msg = Message.obtain();
        if (!isOpen) {
            return;
        }
        int length = MainApplicanton.driver.ReadData(buffer, 4096);
        if (length > 0) {
            String recv = toHexString(buffer, length);
            setTextViewText(recv);
//				String recv = new String(buffer, 0, length);
//            msg.obj = recv;
//            handler.sendMessage(msg);
        }
    }

    private void setTextViewText(String str){
        if(textView!=null)
            textView.append(str);
    }

    private static class MyHandler extends Handler {
        WeakReference<MainActivity> mainActivityWeakReference;

        MyHandler(MainActivity activity) {
            mainActivityWeakReference = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            MainActivity activity = mainActivityWeakReference.get();
            if(activity != null) {
                activity.setTextViewText((String) msg.obj);
            }
        }
    }

    private class ReadThread extends Thread {
        @Override
        public void run() {

                byte[] buffer = new byte[4096];

                while (true) {

                    Message msg = Message.obtain();
                    if (!isOpen) {
                        break;
                    }
                    int length = MainApplicanton.driver.ReadData(buffer, 4096);
                    if (length > 0) {
                        String recv = toHexString(buffer, length);
//					String recv = new String(buffer, 0, length);
                        msg.obj = recv;
                        handler.sendMessage(msg);
                    }
                }
        }
    }


    /**
     * 将byte[]数组转化为String类型
     * @param arg
     *            需要转换的byte[]数组
     * @param length
     *            需要转换的数组长度
     * @return 转换后的String队形
     */
    private String toHexString(byte[] arg, int length) {
        String result = new String();
        if (arg != null) {
            for (int i = 0; i < length; i++) {
                result = result
                        + (Integer.toHexString(
                        arg[i] < 0 ? arg[i] + 256 : arg[i]).length() == 1 ? "0"
                        + Integer.toHexString(arg[i] < 0 ? arg[i] + 256
                        : arg[i])
                        : Integer.toHexString(arg[i] < 0 ? arg[i] + 256
                        : arg[i])) + " ";
            }
            return result;
        }
        return "";
    }


    /**
     * 将String转化为byte[]数组
     * @param arg
     *            需要转换的String对象
     * @return 转换后的byte[]数组
     */
    private byte[] toByteArray(String arg) {
        if (arg != null) {
			/* 1.先去除String中的' '，然后将String转换为char数组 */
            char[] NewArray = new char[1000];
            char[] array = arg.toCharArray();
            int length = 0;
            for (int i = 0; i < array.length; i++) {
                if (array[i] != ' ') {
                    NewArray[length] = array[i];
                    length++;
                }
            }
			/* 将char数组中的值转成一个实际的十进制数组 */
            int EvenLength = (length % 2 == 0) ? length : length + 1;
            if (EvenLength != 0) {
                int[] data = new int[EvenLength];
                data[EvenLength - 1] = 0;
                for (int i = 0; i < length; i++) {
                    if (NewArray[i] >= '0' && NewArray[i] <= '9') {
                        data[i] = NewArray[i] - '0';
                    } else if (NewArray[i] >= 'a' && NewArray[i] <= 'f') {
                        data[i] = NewArray[i] - 'a' + 10;
                    } else if (NewArray[i] >= 'A' && NewArray[i] <= 'F') {
                        data[i] = NewArray[i] - 'A' + 10;
                    }
                }
				/* 将 每个char的值每两个组成一个16进制数据 */
                byte[] byteArray = new byte[EvenLength / 2];
                for (int i = 0; i < EvenLength / 2; i++) {
                    byteArray[i] = (byte) (data[i * 2] * 16 + data[i * 2 + 1]);
                }
                return byteArray;
            }
        }
        return new byte[] {};
    }


    /**
     * 将String转化为byte[]数组
     * @param arg
     *            需要转换的String对象
     * @return 转换后的byte[]数组
     */
    private byte[] toByteArray2(String arg) {
        if (arg != null) {
			/* 1.先去除String中的' '，然后将String转换为char数组 */
            char[] NewArray = new char[1000];
            char[] array = arg.toCharArray();
            int length = 0;
            for (int i = 0; i < array.length; i++) {
                if (array[i] != ' ') {
                    NewArray[length] = array[i];
                    length++;
                }
            }
            NewArray[length] = 0x0D;
            NewArray[length + 1] = 0x0A;
            length += 2;

            byte[] byteArray = new byte[length];
            for (int i = 0; i < length; i++) {
                byteArray[i] = (byte)NewArray[i];
            }
            return byteArray;

        }
        return new byte[] {};
    }
}
