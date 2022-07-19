package eu.darken.myperm.apps.core.container

import eu.darken.myperm.apps.core.Pkg
import eu.darken.myperm.apps.core.features.HasApkData
import eu.darken.myperm.apps.core.features.HasInstallData

interface BasicPkgContainer : Pkg, HasApkData, HasInstallData