package com.example.mineguard.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.example.mineguard.alarm.model.AlarmItem;
import java.util.List;

@Dao
public interface AlarmDao {
    // 插入数据
    @Insert
    void insert(AlarmItem alarm);


    // ============ 【新增】更新方法 ============
    @androidx.room.Update
    void update(AlarmItem alarm);
    // ========================================
    // 查询所有数据，按 dbId 倒序排列 (最新的在最上面)
    // 返回 LiveData，这样界面可以自动更新
    @Query("SELECT * FROM alarm_table ORDER BY dbId DESC")
    LiveData<List<AlarmItem>> getAllAlarms();

    // 清空表 (可选，测试时很有用)
    @Query("DELETE FROM alarm_table")
    void deleteAll();
}