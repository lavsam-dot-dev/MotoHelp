package ru.motohelp.app.utils

import androidx.annotation.StringRes
import ru.motohelp.app.R

enum class PointState {
    Base,
    Checked,
    NotRelevant
}

enum class AccidentType {
    Accident,
    Ambulance,
    Breakdown
}

enum class AccidentDescription {
    None(R.string.unknown_accident),
    CarCollision(R.string.car_collision),
    HittingFooter(R.string.hitting_footer),
    CollisionWithRider(R.string.collision_with_rider),
    LossOfControl(R.string.loss_of_control),
    OtherAccidentDescription(R.string.other_accident_description);

    @StringRes var stringResId : Int

    constructor(@StringRes id : Int) {
        stringResId = id
    }
}

enum class SeverityAccident {
    None(R.string.unknown),
    NoInjury(R.string.no_injury),
    Contusion(R.string.contusion),
    Fracture(R.string.fracture),
    CriticalSituation(R.string.critical_situation),
    Victims(R.string.victims),
    UnknownSeverity(R.string.unknown_severity);

    @StringRes var stringResId : Int

    constructor(@StringRes id : Int) {
        stringResId = id
    }
}