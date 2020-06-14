package com.aqua_ix.locationrecorder

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import java.util.concurrent.TimeUnit

class SwitchFragment : Fragment() {
    // 位置情報が取得可能かチェックする
    private fun checkLocationAvailable() {
        // Activityがnullならなにもしない
        val act = activity ?: return

        val checkRequest =
            LocationSettingsRequest.Builder().addLocationRequest(getLocationRequest()).build()
        val checkTask = LocationServices.getSettingsClient(act).checkLocationSettings(checkRequest)

        checkTask.addOnCompleteListener { response ->
            try {
                response.getResult(ApiException::class.java)
                // 位置情報は取得可能。パーミッションの確認に進む
                checkLocationPermission()
            } catch (exception: ApiException) {
                // 位置情報を取得できないなら例外が投げられる
                if (exception.statusCode == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                    // 解決可能
                    val resolvable = exception as ResolvableApiException
                    resolvable.startResolutionForResult(activity, 1)
                } else {
                    // 解決不可能
                    showErrorMessage()
                }
            }
        }
    }

    // 位置情報取得のパーミッションを得ているかチェックする
    private fun checkLocationPermission() {
        // contextがnullなら何もしない
        val ctx = context ?: return

        // パーミッションを確認
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            // 位置情報のリクエスト開始
            val intent = Intent(ctx, LocationService::class.java)
            val service = PendingIntent.getService(
                ctx, 1,
                intent, PendingIntent.FLAG_UPDATE_CURRENT
            )
            LocationServices.getFusedLocationProviderClient(ctx)
                .requestLocationUpdates(getLocationRequest(), service)

            ctx.getSharedPreferences("LocationRequesting", Context.MODE_PRIVATE)
                .edit().putBoolean("isRequesting", true).apply()

        } else {
            // パーミッションを要求
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }

    // エラーを表示してアクティビティを閉じる
    private fun showErrorMessage() {
        Toast.makeText(context, "位置情報を取得することができません", Toast.LENGTH_SHORT).show()
        activity?.finish()
    }

    private fun getLocationRequest() =
        LocationRequest()
            // 5分くらいの間隔で取得する
            .setInterval(TimeUnit.MINUTES.toMillis(5))
            // 最短で1分は間隔をあける
            .setFastestInterval(TimeUnit.MINUTES.toMillis(1))
            // 最大20分間結果を貯める
            .setMaxWaitTime(TimeUnit.MINUTES.toMillis(20))
            .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            // ユーザーが設定を変更してくれた
            checkLocationPermission()
        } else {
            // 設定を変更してくれなかった
            showErrorMessage()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (permissions.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // パーミッションが付与されたら位置情報のリクエスト開始
            checkLocationPermission()
        } else {
            // パーミッションが付与されなかった
            showErrorMessage()
        }
    }

    private fun stopLocationRequest() {
        val ctx = context ?: return

        val intent = Intent(ctx, LocationService::class.java)
        val service = PendingIntent.getService(
            ctx, 1,
            intent, PendingIntent.FLAG_UPDATE_CURRENT
        )
        LocationServices.getFusedLocationProviderClient(ctx)
            .removeLocationUpdates(service)

        ctx.getSharedPreferences("LocationRequesting", Context.MODE_PRIVATE)
            .edit().putBoolean("isRequesting", false).apply()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_switch, container, false)
        val switch = view.findViewById<Switch>(R.id.locationSwitch)

        // 保存されている位置情報のリクエスト状況に応じて、スイッチのON／OFF初期表示を変える
        val isRequesting = context?.getSharedPreferences("LocationRequesting", Context.MODE_PRIVATE)
            ?.getBoolean("isRequesting", false) ?: false
        switch.isChecked = isRequesting

        // ユーザーがスイッチを切り替えたときの処理
        switch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkLocationAvailable()
            } else {
                stopLocationRequest()
            }
        }

        return view
    }
}