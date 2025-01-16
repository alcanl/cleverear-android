package com.alcanl.cleverear

import androidx.test.platform.app.InstrumentationRegistry
import com.alcanl.cleverear.di.module.ConfigurationModule
import com.alcanl.cleverear.di.module.ProductManagerModule
import com.alcanl.cleverear.entity.EddystoneHearingAid
import com.alcanl.cleverear.fragment.BeaconFragment
import com.alcanl.cleverear.util.EarSide
import com.alcanl.cleverear.util.listeners.BeaconFragmentEventListener
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.Executors

class BeaconFragmentTest {

    private lateinit var configuration : ConfigurationModule

    private lateinit var fragment: BeaconFragment

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp()
    {
        fragment = BeaconFragment()
        fragment.configuration = ConfigurationModule.createConfiguration()
        fragment.listener = BeaconFragmentEventListener(appContext)
        fragment.listener.threadPool = Executors.newSingleThreadExecutor()
        fragment.listener.mEventHandler = ProductManagerModule.createEventHandler()
        configuration = ConfigurationModule.createConfiguration()
    }

    @After
    fun tearDown()
    {
        if (fragment.listener.isListenerOnBoard())
            fragment.listener.stopListener()
    }

    @Test
    fun leftDeviceConnectedAndNotTracking_PressStartTrackingButton_ThenCheckTracking()
    {
        configuration.eddyStoneMap[EarSide.Left] = EddystoneHearingAid()
        fragment.handleEddystoneButtonUI(EarSide.Left)
        assert(configuration.eddyStoneMap[EarSide.Left]?.isTrackingActive!!)
    }

    @Test
    fun rightDeviceConnectedAndNotTracking_PressStartTrackingButton_ThenCheckTracking()
    {
        configuration.eddyStoneMap[EarSide.Right] = EddystoneHearingAid()
        fragment.handleEddystoneButtonUI(EarSide.Right)
        assert(configuration.eddyStoneMap[EarSide.Right]?.isTrackingActive!!)
    }

    @Test
    fun leftDeviceConnectedAndTracking_PressStartTrackingButton_ThenCheckTracking()
    {
        configuration.eddyStoneMap[EarSide.Left] = EddystoneHearingAid()
        configuration.eddyStoneMap[EarSide.Left]?.isTrackingActive = true
        fragment.handleEddystoneButtonUI(EarSide.Left)
        assert(!configuration.eddyStoneMap[EarSide.Left]?.isTrackingActive!!)
    }

    @Test
    fun rightDeviceConnectedAndTracking_PressStartTrackingButton_ThenCheckTracking()
    {
        configuration.eddyStoneMap[EarSide.Right] = EddystoneHearingAid()
        configuration.eddyStoneMap[EarSide.Right]?.isTrackingActive = true
        fragment.handleEddystoneButtonUI(EarSide.Right)
        assert(!configuration.eddyStoneMap[EarSide.Right]?.isTrackingActive!!)

    }

    @Test
    fun startListener_ThenCheckThread()
    {
        fragment.listener.startEventListener()
        assert(fragment.listener.isListenerOnBoard())
    }

}
