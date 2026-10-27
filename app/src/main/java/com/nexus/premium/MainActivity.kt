<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#09090B"
    android:fillViewport="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="24dp">

        <!-- LOGO & TITLE -->
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="NEXUS PREMIUM"
            android:textColor="#00F3FF"
            android:textSize="28sp"
            android:textStyle="bold"
            android:fontFamily="monospace"
            android:layout_gravity="center_horizontal"
            android:shadowColor="#00F3FF"
            android:shadowDx="0" android:shadowDy="0" android:shadowRadius="15" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="HỆ ĐIỀU HÀNH GIẢI ĐỀ"
            android:textColor="#666666"
            android:textSize="12sp"
            android:letterSpacing="0.2"
            android:layout_gravity="center_horizontal"
            android:layout_marginBottom="32dp"/>

        <!-- API KEY CARD -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:background="@drawable/bg_card"
            android:padding="16dp"
            android:layout_marginBottom="16dp">

            <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
                android:text="GROQ API KEYS" android:textColor="#00F3FF" android:textSize="12sp" android:textStyle="bold" android:layout_marginBottom="8dp"/>

            <EditText
                android:id="@+id/etKey1"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:hint="Nhập Key 1 (Bắt buộc)"
                android:textColor="#FFF" android:textColorHint="#444" android:textSize="14sp"
                android:backgroundTint="#00F3FF" android:layout_marginBottom="8dp" android:inputType="textPassword"/>

            <EditText
                android:id="@+id/etKey2"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:hint="Nhập Key 2 (Tùy chọn)"
                android:textColor="#FFF" android:textColorHint="#444" android:textSize="14sp"
                android:backgroundTint="#333" android:layout_marginBottom="8dp" android:inputType="textPassword"/>

            <EditText
                android:id="@+id/etKey3"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:hint="Nhập Key 3 (Tùy chọn)"
                android:textColor="#FFF" android:textColorHint="#444" android:textSize="14sp"
                android:backgroundTint="#333" android:inputType="textPassword"/>

            <Button
                android:id="@+id/btnSaveKey"
                android:layout_width="match_parent"
                android:layout_height="50dp"
                android:layout_marginTop="12dp"
                android:text="LƯU CẤU HÌNH"
                android:background="@drawable/bg_btn_neon"
                android:textColor="#000" android:textStyle="bold"/>
        </LinearLayout>

        <!-- TRẠNG THÁI HỆ THỐNG -->
        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="TRẠNG THÁI HỆ THỐNG" android:textColor="#888" android:textSize="12sp" android:layout_marginBottom="8dp"/>

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:background="@drawable/bg_card"
            android:padding="16dp">

            <!-- Quyền thông báo -->
            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="horizontal" android:gravity="center_vertical" android:layout_marginBottom="12dp">
                <TextView android:id="@+id/tvNotiStatus" android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:text="1. Quyền Thông Báo" android:textColor="#FFF"/>
                <Button android:id="@+id/btnNoti" android:layout_width="wrap_content" android:layout_height="40dp" android:text="CẤP QUYỀN" android:textSize="10sp" android:backgroundTint="#333"/>
            </LinearLayout>

            <!-- Quyền trợ năng -->
            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="horizontal" android:gravity="center_vertical" android:layout_marginBottom="12dp">
                <TextView android:id="@+id/tvAccStatus" android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:text="2. Quyền Trợ Năng" android:textColor="#FFF"/>
                <Button android:id="@+id/btnAcc" android:layout_width="wrap_content" android:layout_height="40dp" android:text="CẤP QUYỀN" android:textSize="10sp" android:backgroundTint="#333"/>
            </LinearLayout>

            <!-- Quyền nổi -->
            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="horizontal" android:gravity="center_vertical">
                <TextView android:id="@+id/tvOverlayStatus" android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:text="3. Quyền Cửa Sổ Nổi" android:textColor="#FFF"/>
                <Button android:id="@+id/btnOverlay" android:layout_width="wrap_content" android:layout_height="40dp" android:text="CẤP QUYỀN" android:textSize="10sp" android:backgroundTint="#333"/>
            </LinearLayout>
        </LinearLayout>

        <Button
            android:id="@+id/btnStart"
            android:layout_width="match_parent"
            android:layout_height="60dp"
            android:layout_marginTop="24dp"
            android:text="🚀 KHỞI CHẠY NEXUS"
            android:textStyle="bold" android:textSize="16sp"
            android:background="@drawable/bg_btn_neon"
            android:textColor="#000"/>

    </LinearLayout>
</ScrollView>
