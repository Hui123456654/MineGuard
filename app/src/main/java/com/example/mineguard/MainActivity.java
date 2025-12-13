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
import okhttp3.*;
import java.io.IOException;
import android.widget.Toast;



public class MainActivity extends AppCompatActivity {

    private final String[] CAMERA_IPS = {
            "192.168.1.64",
            "192.168.1.65",
            "192.168.1.66",
            "192.168.1.67"
    };
    // 对应的 ID（必须与 IP 顺序一致）
    private  String[] CAMERA_IDS = {"CAM_01", "CAM_02", "CAM_03", "CAM_04"};
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

    private void configureAllCameras() {
        String myIp ="192.168.1.2"; // 获取本机 IP (方法需自行实现，见下文)
        int myPort = 8888;
        String myUri = "/alarm/info"; // 这里必须与 LocalServer 里的判断一致

        OkHttpClient client = new OkHttpClient();

        for (int i = 0; i < CAMERA_IPS.length; i++) {
            String ip = CAMERA_IPS[i];
            String id = CAMERA_IDS[i];

            // 开启设备报警信息上传 URL
            String url = "http://" + ip + ":5002/server/set/smart/event/on/";

            // 参数构造
            RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("protocol", "http")
                    .addFormDataPart("port", String.valueOf(myPort))
                    .addFormDataPart("uri", myUri)
                    .addFormDataPart("extend", id) // 关键：用这个区分是哪个相机
                    .build();

            Request request = new Request.Builder().url(url).post(body).build();

            // 异步发送
            final String currentIp = ip;
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "配置失败: " + currentIp, Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "配置成功: " + currentIp, Toast.LENGTH_SHORT).show());
                }
            });
        }
    }
}