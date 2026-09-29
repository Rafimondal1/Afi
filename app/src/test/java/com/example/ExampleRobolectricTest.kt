package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("TradeView Pro", appName)
  }

  @Test
  fun `test Binance klines fetch`() = kotlinx.coroutines.runBlocking {
    val candles = com.example.data.api.RealMarketApiService.fetchBinanceKlines("BTCUSDT", com.example.model.Timeframe.MIN_1, 10)
    println("Binance BTC candles count: ${candles.size}")
    if (candles.isNotEmpty()) {
      println("Latest BTC candle close: ${candles.last().close}")
    }
  }

  @Test
  fun `test Yahoo GC=F fetch`() = kotlinx.coroutines.runBlocking {
    val (ticker, candles) = com.example.data.api.RealMarketApiService.fetchYahooChart("GC=F", com.example.model.Timeframe.MIN_15)
    println("Yahoo GC=F ticker: $ticker")
    println("Yahoo GC=F candles count: ${candles.size}")
  }

  @Test
  fun `test Yahoo NSEI fetch`() = kotlinx.coroutines.runBlocking {
    val (ticker, candles) = com.example.data.api.RealMarketApiService.fetchYahooChart("^NSEI", com.example.model.Timeframe.MIN_15)
    println("Yahoo ^NSEI ticker: $ticker")
    println("Yahoo ^NSEI candles count: ${candles.size}")
  }
}
