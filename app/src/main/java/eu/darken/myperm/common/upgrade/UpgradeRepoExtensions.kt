package eu.darken.myperm.common.upgrade

import kotlinx.coroutines.flow.first


suspend fun UpgradeRepo.isPro(): Boolean = upgradeInfo.first().isPro