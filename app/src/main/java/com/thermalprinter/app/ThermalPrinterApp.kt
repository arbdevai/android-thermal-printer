package com.thermalprinter.app

import android.app.Application
import com.thermalprinter.app.data.AppData
import com.thermalprinter.app.printer.BluetoothPrinterManager

class ThermalPrinterApp : Application() {
    lateinit var appData: AppData
        private set

    lateinit var printerManager: BluetoothPrinterManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        appData = AppData(this)
        printerManager = BluetoothPrinterManager(this)
    }

    companion object {
        lateinit var instance: ThermalPrinterApp
            private set
    }
}
