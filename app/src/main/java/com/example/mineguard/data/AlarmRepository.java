package com.example.mineguard.data;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.example.mineguard.alarm.model.AlarmItem;
import java.util.List;

public class AlarmRepository {
    private AlarmDao alarmDao;
    private LiveData<List<AlarmItem>> allAlarms;

    public AlarmRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        alarmDao = db.alarmDao();
        allAlarms = alarmDao.getAllAlarms();
    }

    // 给 ViewModel 用的：获取所有数据
    public LiveData<List<AlarmItem>> getAllAlarms() {
        return allAlarms;
    }

    // 给 MainActivity 用的：插入数据 (在后台线程执行)
    public void insert(AlarmItem alarm) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            alarmDao.insert(alarm);
        });
    }

    // ============ 【新增】Repository 层的更新逻辑 ============
    public void update(AlarmItem alarm) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            alarmDao.update(alarm);
        });
    }
    // ======================================================
}