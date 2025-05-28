# 原文介绍地址：
我的首个AI程序——安卓Usb通信收发测试（附源码）https://ranjuan.cn/deepseek-android-usb-test/

## 主要功能：

1、选择符合条件的usb设备，建立usb连接  

2、选择或手动输入hex字符并发送给已连接的设备  

3、接收usb的返回信息（比如发送查询usb状态的hex字符后，再点接收数据按钮）  

## Bug告知：
1、已知bug为 如果在常规手机上通过usb集线器同时连接多个usb，可能会引起usb授权后闪退；建议一个usb设备连接授权后再连下一个  
2、如果是有root权限的安卓开发板，一般都是默认有权限也不会弹出授权窗口，则不会存在此bug  
3、注意修改源代码中的设备标识：  
***USBActivity.java文件下的TARGET_VENDOR_ID、TARGET_PRODUCT_ID：***  
    
      private static final int TARGET_VENDOR_ID = 0x5A5A;  
      private static final int TARGET_PRODUCT_ID = 0x8009;  
   

***res/xml/device_filter.xml 下的过滤设备标识:***  
``<usb-device  vendor-id="0x5A5A" product-id="0x8009" />``  

