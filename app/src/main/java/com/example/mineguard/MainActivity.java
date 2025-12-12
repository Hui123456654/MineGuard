package com.example.mineguard;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import com.example.mineguard.data.DeviceRepository;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.example.mineguard.analysis.AnalysisFragment;  // 导入 智能分析
import com.example.mineguard.alarm.AlarmFragment;
import com.example.mineguard.configuration.ConfigurationFragment;  // 导入 配置页
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private Fragment[] fragments = new Fragment[3];
    private int currentIndex = 0;
    private WindowInsetsControllerCompat windowInsetsController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DeviceRepository.init(getApplicationContext());
        // ----------------------------------------------------
        // !!! 临时调试代码：执行设备数据重置 !!!
        //DeviceRepository.getInstance().resetData();
        // 重置后，请将此行代码注释掉或删除，否则每次启动都会重置数据！
        // ----------------------------------------------------
        setContentView(R.layout.activity_main);

        // 初始化控制器
        windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());

        // 初始化 Fragment
        fragments[0] = new AnalysisFragment(); // 新增的智能分析页
        fragments[1] = new AlarmFragment();
        fragments[2] = new ConfigurationFragment();

        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, fragments[0])
                .commit();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            int newIndex = -1;

            if (id == R.id.nav_analysis) newIndex = 0;
            else if (id == R.id.nav_alarms) newIndex = 1;
            else if (id == R.id.nav_configuration) newIndex = 2;

            if (newIndex != -1 && newIndex != currentIndex) {
                switchFragment(currentIndex, newIndex);
                currentIndex = newIndex;
                return true;
            }
            return false;
        });
    }

    private void switchFragment(int oldIndex, int newIndex) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        Fragment oldFrag = fragments[oldIndex];
        Fragment newFrag = fragments[newIndex];

        if (!newFrag.isAdded()) {
            transaction.hide(oldFrag).add(R.id.fragment_container, newFrag);
        } else {
            transaction.hide(oldFrag).show(newFrag);
        }
        transaction.commit();
    }
}