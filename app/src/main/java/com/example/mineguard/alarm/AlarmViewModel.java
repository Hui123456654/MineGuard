package com.example.mineguard.alarm;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.example.mineguard.alarm.model.AlarmItem;
import com.example.mineguard.data.AlarmRepository;
import java.util.List;

public class AlarmViewModel extends AndroidViewModel {
    private AlarmRepository repository;
    private LiveData<List<AlarmItem>> allAlarms;

    public AlarmViewModel(@NonNull Application application) {
        super(application);
        repository = new AlarmRepository(application);
        allAlarms = repository.getAllAlarms();
    }

    public LiveData<List<AlarmItem>> getAllAlarms() {
        return allAlarms;
    }

    // 如果需要在 UI 层插入数据，也可以调这个
    public void insert(AlarmItem alarm) {
        repository.insert(alarm);
    }

    // ============ 【新增】ViewModel 层的更新逻辑 ============
    public void update(AlarmItem alarm) {
        repository.update(alarm);
    }
    // ======================================================
}