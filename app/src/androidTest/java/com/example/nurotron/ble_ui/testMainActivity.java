package com.example.nurotron.ble_ui;

import android.test.ActivityInstrumentationTestCase2;

import com.nurotron.ble_ui.MainActivity;
import com.nurotron.ble_ui.R;
import com.robotium.solo.Solo;

import android.test.suitebuilder.annotation.MediumTest;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import static android.content.ContentValues.TAG;


/**
 * Created by sxl on 2018/6/26.
 */

public class testMainActivity extends ActivityInstrumentationTestCase2<MainActivity> {

//    初始化测试对象
    private Solo solo;
//    在构造函数处标明继承自目标项目的启动类
    public testMainActivity(){
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception{
        super.setUp();
        solo = new Solo(getInstrumentation(),getActivity());
    }

    @Override
    protected void tearDown() throws Exception{
        solo.finishOpenedActivities();
    }

    @MediumTest
    public void testMapChange() throws Exception{
//       有个问题，未连上设备时，还未处理
//       已连接的时候，切换map，判断map是否切换成功

//        solo.unlockScreen();
        solo.waitForActivity("MainActivity",30000);//等待MainActivity的启动
        solo.assertCurrentActivity("Expected MainActivity activity",MainActivity.class);
//        assertTrue("无法启动类",solo.waitForActivity("MainActivity",30000));
        solo.goBackToActivity("MainActivity");
        solo.sleep(8000);
        
//        solo.clickOnText("程序二");
        //clickOnToggleButton只能按名称点，可以按坐标点防止找不到名称
        solo.clickOnToggleButton("程序三");
        solo.sleep(2000);

        //判断第三个toggleButton是否选择
        boolean mapcheck3 =solo.isToggleButtonChecked(2);
        assertTrue("map 3 is not checked",mapcheck3);

    }

    public void testMapName() throws Exception{
//        测试修改map名称
        //还没有做输入范围的测试
        solo.drag(340,30,200,200,3);
        solo.sleep(1000);

        TextView text = (EditText)solo.getView(R.id.programText);

        solo.clickOnEditText(0);
        solo.clearEditText(0);
        solo.enterText(0,"222");

        solo.hideSoftKeyboard();
        solo.drag(100,340,200,200,3);
//        solo.takeScreenshot(String name);
//        保存截屏并设置名字
        solo.sleep(3000);

        solo.assertCurrentActivity("Expected MainActivity activity",MainActivity.class);
        boolean txt = solo.searchText("222");
        assertTrue("修改名称失败",txt);

    }

}

