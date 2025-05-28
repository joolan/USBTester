package com.example.usbtester;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Filter;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ArrayAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import android.os.Bundle;
/*
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}*/

public class USBActivity extends AppCompatActivity {
    private static final String TAG = "USBTest";
    // 设备标识常量
    private static final int TARGET_VENDOR_ID = 0x5A5A;
    private static final int TARGET_PRODUCT_ID = 0x8009;

    private static final long CONNECTION_INVALID = -1L;
    private long lastConnectTime = CONNECTION_INVALID; // 初始化为无效状态
    private static final int USB_PERMISSION_REQUEST_CODE = 1001;
    // 添加连接时间记录
    private static final String ACTION_USB_PERMISSION = "android.hardware.usb.action.USB_PERMISSION";
    private List<UsbDevice> matchedDevices = new ArrayList<>();
    private ArrayAdapter<String> hexAdapter; // 添加这行
    private UsbDevice selectedDevice;
    private boolean isConnected = false;
    private UsbManager usbManager;
    private UsbDeviceConnection connection;
    private UsbInterface usbInterface;
    private UsbEndpoint endpointIn;
    private UsbEndpoint endpointOut;
    private TextView logTextView;
    private AutoCompleteTextView hexInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化控件
        logTextView = findViewById(R.id.log_text);
        Button btnSelect = findViewById(R.id.btn_select);
        Button btnConnect = findViewById(R.id.btn_connect);
        // 初始化Hex输入控件
        initHexInput();

        // 初始化清除按钮
        findViewById(R.id.btn_clear).setOnClickListener(v -> clearLogs());
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        // 设备选择按钮
        //btnSelect.setOnClickListener(v -> showDeviceList());

        // 初始化连接按钮
        //findViewById(R.id.btn_connect).setOnClickListener(v -> manualConnectDevice());
        //checkConnectedDevices();
        // 连接按钮
        /*btnConnect.setOnClickListener(v -> {
            if (selectedDevice != null) {
                connectSelectedDevice();
            } else {
                log("请先选择设备");
            }
        });*/
    }
    private void initHexInput() {
        hexInput = findViewById(R.id.hex_input);
        // 加载预设数据
        String[] presets = getResources().getStringArray(R.array.hex_presets);
        // 使用链表结构方便数据操作
        List<String> presetList = new LinkedList<>(Arrays.asList(presets));
        /*hexAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                presets
        );*/
        // 使用自定义过滤器
        hexAdapter = new ArrayAdapter<String>(
                this,
                R.layout.dropdown_item,
                R.id.text1,
                presetList
        ){
            @Override
            public Filter getFilter() {
                return new NoFilter(presetList);
            }
        };
        // 设置关键属性
        hexInput.setThreshold(1); // 输入1个字符即触发
        //hexAdapter.setNotifyOnChange(true);
        hexInput.setAdapter(hexAdapter);
        hexInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) hexInput.showDropDown();
        });

        // 设置点击监听
        hexInput.setOnClickListener(v -> {
            if (!hexInput.isPopupShowing()) {
                hexInput.showDropDown();
            }
        });
        // 添加文本监听
        hexInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // 必须实现但无需操作
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 必须实现但无需操作
            }
            @Override
            public void afterTextChanged(Editable s) {
                // 确保在主线程操作
                runOnUiThread(() -> {
                    if (hexInput == null) return;

                    try {
                        if (s.length() > 0 && !hexInput.isPopupShowing()) {
                            // 延迟显示确保输入法不影响布局
                            hexInput.postDelayed(() -> {
                                if (hexInput != null) {
                                    hexInput.showDropDown();
                                }
                            }, 100);
                        }
                    } catch (Exception e) {
                        log( "显示下拉列表失败");
                    }
                });
            }
            // 其他空方法...
        });

        /*hexInput.setAdapter(hexAdapter);
        hexInput.setOnItemClickListener((parent, view, position, id) -> {
            String selected = hexAdapter.getItem(position);
            hexInput.setText(selected);
        });*/
    }
    // 自定义不过滤的过滤器
    // 在 Activity 类内部添加
    private class NoFilter extends Filter {
        private final Object lock = new Object();
        private final List<String> originalData;

        public NoFilter(List<String> data) {
            this.originalData = new ArrayList<>(data);
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            synchronized (lock) {
                results.values = originalData;
                results.count = originalData.size();
            }
            return results;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void publishResults(CharSequence constraint, FilterResults results) {
            if (hexAdapter != null) {
                hexAdapter.clear();
                hexAdapter.addAll((List<String>) results.values);
                hexAdapter.notifyDataSetChanged();
            }
        }
    }
    // 新增清除日志方法
    private void clearLogs() {
        runOnUiThread(() -> {
            logTextView.setText("日志输出："); // 重置为初始状态
            logTextView.scrollTo(0, 0);    // 滚动到顶部
        });
    }
    private void updateUIStatus() {
        runOnUiThread(() -> {
            boolean connected = (connection != null);
            findViewById(R.id.btn_connect).setEnabled(!connected);
            findViewById(R.id.btn_send).setEnabled(connected);
            findViewById(R.id.btn_receive).setEnabled(connected);
        });
    }
    // 显示设备列表对话框
    // 修改后的设备选择方法
    private void showDeviceList() {
        scanUsbDevices();

        // 增强空列表处理
        if (matchedDevices.isEmpty()) {
            log("没有可用的目标设备");
            return;
        }

        // 安全生成设备信息列表
        List<String> deviceInfos = new ArrayList<>();
        for (UsbDevice device : matchedDevices) {
            String serial = device.getSerialNumber() != null ?
                    device.getSerialNumber() : "未知序列号";
            deviceInfos.add("设备ID:" + device.getDeviceId() + " S/N:" + serial);
        }

        try {
            new AlertDialog.Builder(this)
                    .setTitle("选择设备（共" + matchedDevices.size() + "台）")
                    .setItems(deviceInfos.toArray(new String[0]), (dialog, which) -> {
                        if (which >= 0 && which < matchedDevices.size()) {
                            releaseConnection(); // 先释放旧连接

                            // 等待物理层稳定（关键！）
                            try {
                                log("已选择新设备，暂停300ms后继续……");
                                Thread.sleep(300);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            selectedDevice = matchedDevices.get(which);
                            connectSelectedDevice();// 立即触发新连接
                            log("已选择设备：" + deviceInfos.get(which));
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
        } catch (Exception e) {
            log("对话框创建失败: " + e.getMessage());
        }
    }

    // 扫描USB设备
    // 增强设备扫描方法
    private void scanUsbDevices() {
        matchedDevices.clear();
        HashMap<String, UsbDevice> deviceList = null;

        try {
            // 1. 前置安全检查
            if (usbManager == null) {
                log("USB服务不可用");
                return;
            }

            // 2. 获取设备列表（可能抛出多个异常）
            deviceList = usbManager.getDeviceList();
        } catch (SecurityException e) {
            log("USB访问权限被拒绝");
            //showPermissionDialog(); // 显示引导用户授权的界面
            return;
        }catch (NullPointerException e) {
            log("系统服务异常: " + e.getMessage());
            return;
        } catch (RuntimeException e) {
            log("未知运行时错误: " + e.getClass().getSimpleName());
            return;
        }finally {
            // 3. 异常后资源清理
            if (deviceList == null) {
                releaseUsbResources(); // 释放已分配资源
            }
        }

        if (deviceList == null || deviceList.isEmpty()) {
            log("未检测到任何USB设备");
            return;
        }

        // 5. 设备处理（添加try-catch）
        for (UsbDevice device : deviceList.values()) {
            try {
                processDevice(device); // 封装设备处理逻辑
            } catch (Exception e) {
                log("设备处理异常: " + device.getDeviceName());
                //Log.w(TAG, "设备处理错误", e);
            }
        }

        log("找到匹配设备数: " + matchedDevices.size());
    }
    // 独立设备处理方法
    private void processDevice(UsbDevice device) {
        // 设备信息校验
        if (device.getVendorId() == 0 || device.getProductId() == 0) {
            throw new IllegalArgumentException("无效设备ID");
        }

        // 设备匹配逻辑
        if (isTargetDevice(device)) {
            synchronized (matchedDevices) {
                matchedDevices.add(device);
            }
            //logDeviceInfo(device);
        }
    }
    // 安全释放资源
    private void releaseUsbResources() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (Exception e) {
            //Log.w(TAG, "连接关闭异常", e);
            log("连接关闭异常");
        } finally {
            connection = null;
            selectedDevice = null;
        }
    }
    // 连接选中的设备
    private void connectSelectedDevice() {
        if (usbManager.hasPermission(selectedDevice)) {
            log("已有权限，开始连接...");
            setupConnection(selectedDevice);
        } else {
            log("正在请求设备权限...");
            requestDevicePermission(selectedDevice);
        }
    }
    // 请求设备权限
    private void requestDevicePermission(UsbDevice device) {
        PendingIntent permissionIntent = PendingIntent.getBroadcast(
                this,
                0,
                new Intent(ACTION_USB_PERMISSION),
                PendingIntent.FLAG_IMMUTABLE
        );
        usbManager.requestPermission(device, permissionIntent);
    }

    // 新增手动连接方法
    private void manualConnectDevice() {
        log("正在扫描USB设备...");
        checkConnectedDevices();
    }
    // 修改后的设备检测方法
    private void checkConnectedDevices() {
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        if (deviceList.isEmpty()) {
            log("未找到连接的USB设备");
            return;
        }

        boolean found = false;
        for (UsbDevice device : deviceList.values()) {
            //if (device.getVendorId() == TARGET_VENDOR_ID && device.getProductId() == TARGET_PRODUCT_ID) {
            if (isTargetDevice(device)){
                log("发现目标设备: " + device.getDeviceName());
                found = true;
                requestPermission(device);
            }
        }

        if (!found) {
            log("未找到匹配的USB设备");
        }
    }

    // 请求USB权限
    private void requestPermission(UsbDevice device) {
        PendingIntent permissionIntent = PendingIntent.getBroadcast(
                this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
        usbManager.requestPermission(device, permissionIntent);
    }

    // 初始化USB连接
    private void setupConnection(UsbDevice device) {
        try {
            //releaseConnection(); // 双重保险
            connection = usbManager.openDevice(device);
            if (connection == null) {
                log("连接失败：无法打开设备");
                lastConnectTime = CONNECTION_INVALID; // 失败时标记无效
                return;
            }

            // 获取接口（通常第一个接口）
            usbInterface = device.getInterface(0);
            if (!connection.claimInterface(usbInterface, true)) {
                log("连接失败：无法声明接口");
                lastConnectTime = CONNECTION_INVALID; // 失败时标记无效
                connection.close();
                return;
            }
            connection.claimInterface(usbInterface, true);

            // 查找端点
            for (int i = 0; i < usbInterface.getEndpointCount(); i++) {
                UsbEndpoint endpoint = usbInterface.getEndpoint(i);
                if (endpoint.getDirection() == UsbConstants.USB_DIR_IN) {
                    endpointIn = endpoint;
                } else if (endpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
                    endpointOut = endpoint;
                }
            }
        }catch (Exception e) {
            log("连接异常: " + e.getMessage());
            /*if (connection != null) {
                connection.close();
                connection = null;
            }*/
            lastConnectTime = CONNECTION_INVALID; // 失败时标记无效
            releaseConnection(); // 异常时清理
            return;
        }
        // 记录连接成功时间
        lastConnectTime = SystemClock.elapsedRealtime();
        //log("设备连接成功");
        log("设备连接成功，时间戳: " + lastConnectTime);
        isConnected = true;
        sendInitializationCommand(); // 如果每次切换usb 第1、2个数据指令可能会丢失或未回复，则初始化指令可以先尝试一次
        updateUIStatus();//更新ui
    }

    // 初始化
    private void sendInitializationCommand() {
        return;
        // 发送设备特定初始化序列
        /*byte[] initCmd = {0x1B, 0x40};//初始化打印机 ESC @ 清除打印缓冲区中的数据，复位打印机模式到电源打开时打印机的有效模式。
        int retry = 0;

        while (retry++ < 3) {
            int result = connection.bulkTransfer(endpointOut, initCmd, initCmd.length, 100);
            if (result == initCmd.length) {
                log("初始化指令发送成功");
                break;
            }
           log( "初始化指令重试:" + retry);
        }*/
    }


    // 发送数据（示例）
    public void sendData_old(View view) {
        if (connection == null || endpointOut == null) {
            log("未正确初始化连接");
            return;
        }

        //byte[] data = "TestData".getBytes();
        //byte[] data = "TestData".getBytes();
        // 修改后的十六进制数据发送
        byte[] data = hexStringToByteArray("AA040155"); // 去掉空格更规范

        int transferred = connection.bulkTransfer(endpointOut, data, data.length, 600);
        log("发送数据: " + Arrays.toString(data) + ", 结果: " + transferred);
    }

    // 添加发送历史记录
    private void saveToHistory(String hex) {
        // 直接遍历适配器判断存在性
        boolean exists = false;
        for (int i = 0; i < hexAdapter.getCount(); i++) {
            if (hexAdapter.getItem(i).equals(hex)) {
                exists = true;
                break;
            }
        }

        if (!exists) {
            hexAdapter.add(hex);
            // 可选：限制历史记录数量
            if (hexAdapter.getCount() > 20) {
                hexAdapter.remove(hexAdapter.getItem(0));
            }
            hexAdapter.notifyDataSetChanged();
        }
    }

    private boolean isDeviceReady() {

        return connection != null
                && usbInterface != null
                && endpointOut != null
                && lastConnectTime != CONNECTION_INVALID
                && (SystemClock.elapsedRealtime() - lastConnectTime) > 300; // 确保300ms初始化
    }
    // 修改后的发送方法
    public void sendData(View view) {
        /*if (connection == null || endpointOut == null ||  usbInterface == null) {
            log("未正确初始化连接");
            return;
        }*/

        // 1. 设备就绪检查
        if (!isDeviceReady()) {
            log( "设备未就绪，延迟发送");
            // 2. 创建延迟任务
            new Handler(Looper.getMainLooper()).postDelayed(
                    () -> sendData(view), // 3. 递归调用
                    150 // 延迟150ms
            );
            return;
        }


        // 获取输入内容并验证
        String input = hexInput.getText().toString().trim();
        if (TextUtils.isEmpty(input)) {
            log("请输入Hex数据");
            return;
        }

        // 转换并发送
        try {
            byte[] data = hexStringToByteArray(input);
            if (data == null || data.length == 0) {
                log("数据转换失败");
                return;
            }

            int transferred = connection.bulkTransfer(endpointOut, data, data.length, 600);
            log("发送结果: " + transferred + " bytes | 原始数据: " + formatHexString(data));
            // 在发送成功后调用
            saveToHistory(input);
        } catch (Exception e) {
            log("发送失败: " + e.getMessage());
        }
    }

    // 接收数据（示例）
    public void receiveData(View view) {
        if (connection == null || endpointIn == null) {
            log("未正确初始化连接");
            return;
        }

        byte[] buffer = new byte[64];
        int received = connection.bulkTransfer(endpointIn, buffer, buffer.length, 500);
        if (received > 0) {
            byte[] validData = Arrays.copyOf(buffer, received);
            log("收到数据: " + Arrays.toString(validData));
        }
    }
    private boolean isTargetDevice(UsbDevice device) {
        // return device.getVendorId() == TARGET_VENDOR_ID && device.getProductId() == TARGET_PRODUCT_ID;
        return device != null
                && device.getVendorId() == TARGET_VENDOR_ID
                && device.getProductId() == TARGET_PRODUCT_ID;
    }
    // 广播接收器处理权限请求
    // 修改后的广播接收器
    // 修改广播接收器
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                if (ACTION_USB_PERMISSION.equals(action)) {
                    handlePermissionResponse(intent);//取消广播触发行为
                } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                    handleDeviceAttached(intent);//取消广播触发行为
                } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                    handleDeviceDetached(intent);//取消广播触发行为
                }
            }catch (Exception e) {
                log("广播处理异常: " + e.getMessage());
            }
        }

        private void handlePermissionResponse(Intent intent) {
            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                if (device != null && device.equals(selectedDevice)) {
                    setupConnection(device);
                }
            } else {
                log("权限被拒绝: " + device.getSerialNumber());
            }
        }

        private void handleDeviceAttached(Intent intent) {
            UsbDevice newDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (isTargetDevice(newDevice)) {
                log("新设备接入: " + newDevice.getSerialNumber());
                if (selectedDevice != null && selectedDevice.equals(newDevice)) {
                    autoReconnect(newDevice);
                }
            }
        }

        private void autoReconnect(UsbDevice device) {
            new Handler().postDelayed(() -> {
                if (!usbManager.hasPermission(device)) {
                    requestDevicePermission(device);
                }
            }, 1000);
        }

        private void handleDeviceDetached(Intent intent) {
            UsbDevice detachedDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (detachedDevice.equals(selectedDevice)) {
                log("当前设备已断开");
                releaseConnection();
            }
        }
    };
    // 释放连接
    private void releaseConnection() {
        if (connection != null) {
            connection.releaseInterface(usbInterface);
            connection.close();
            connection = null;
            endpointIn = null; //释放的时候把端点也释放
            endpointOut = null;//释放的时候把端点也释放
        }
        selectedDevice = null;
        log("usb连接已释放(如有)");
        updateUIStatus();
    }

    // 格式化为带空格的Hex字符串
    private String formatHexString(byte[] data) {
        if (data == null) return "";
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
    // 十六进制字符串转字节数组方法
    private byte[] hexStringToByteArray(String hex) {
        try {
            // 移除所有空格
            //String cleanedHex = hex.replaceAll("\\s", "");
            String cleanedHex = hex.replaceAll("[^0-9A-Fa-f]", "");

            // 校验长度是否为偶数
            if (cleanedHex.length() % 2 != 0) {
                throw new IllegalArgumentException("无效的十六进制字符串");
            }

            byte[] data = new byte[cleanedHex.length() / 2];
            for (int i = 0; i < data.length; i++) {
                int index = i * 2;
                int value = Integer.parseInt(cleanedHex.substring(index, index + 2), 16);
                data[i] = (byte) value;
            }
            return data;
        } catch (Exception e) {
            log("十六进制转换错误: " + e.getMessage());
            return null;
        }
    }

    // 字节数组转十六进制字符串（用于日志）
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }



    // 修改后的onResume方法
    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(usbReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(usbReceiver);
    }

    private void log(final String message) {
        //runOnUiThread(() -> logTextView.append("\n" + message));
        runOnUiThread(() -> {
            // 添加日志内容
            String timestamp = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
                    .format(new Date());
            logTextView.append("\n[" + timestamp + "] " + message);

            // 自动滚动到底部
            final ScrollView scrollContainer = findViewById(R.id.scroll_container);
            scrollContainer.post(() -> {
                scrollContainer.fullScroll(View.FOCUS_DOWN);
                logTextView.requestLayout();
            });
        });
    }


    public void onConnectClick(View view) {
        if (selectedDevice == null) {
            log("请先选择设备");
            return;
        }
        releaseConnection(); // 在连接前强制释放,确保释放旧连接

        if (!usbManager.hasPermission(selectedDevice)) {
            log("正在请求设备权限...");
            requestDevicePermission(selectedDevice);
        } else {
            log("已有权限，开始连接...");
            setupConnection(selectedDevice);
        }

    }

    public void onSelectClick(View view) {
        showDeviceList();
    }
}