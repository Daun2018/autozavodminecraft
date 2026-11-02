package ru.avtovazmod.entity;

/**
 * Параметры ходовых качеств машины. Все скорости — в блоках за тик (1 блок/тик = 20 блоков/с).
 *
 * @param maxSpeed            максимальная скорость вперёд
 * @param acceleration        разгон (затухает по мере приближения к maxSpeed)
 * @param maxReverse          максимальная скорость назад
 * @param reverseAcceleration разгон назад
 * @param brake               сила торможения (блок/тик за тик)
 * @param turnRate            макс. угол поворота корпуса, градусов за тик (при оптимальной скорости)
 * @param grip                сцепление боковое: доля бокового скольжения, гасимая за тик (0..1)
 * @param handbrakeGrip       то же, но с зажатым ручником (меньше = сильнее занос)
 * @param rearWheelDrive      задний привод: на скорости с газом и рулём срывается в занос
 * @param looseSurfaceFactor  множитель скорости на песке/снегу/грязи (1.0 = не теряет скорость)
 * @param stepHeight          высота, на которую машина въезжает без прыжка (блоки)
 * @param wheelRadius         радиус колеса в блоках (для анимации вращения)
 * @param seatFrontFwd        смещение центра передних сидений вдоль курса (блоки, минус = назад)
 * @param seatRearFwd         то же для задних
 * @param seatYOffset         смещение ног пассажира по вертикали относительно низа машины
 */
public record CarStats(
        double maxSpeed,
        double acceleration,
        double maxReverse,
        double reverseAcceleration,
        double brake,
        float turnRate,
        double grip,
        double handbrakeGrip,
        boolean rearWheelDrive,
        double looseSurfaceFactor,
        float stepHeight,
        float wheelRadius,
        double seatFrontFwd,
        double seatRearFwd,
        double seatYOffset
) {
}
