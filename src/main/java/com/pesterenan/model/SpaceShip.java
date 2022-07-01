package com.pesterenan.model;

import krpc.client.Connection;
import krpc.client.RPCException;
import krpc.client.Stream;
import krpc.client.StreamException;
import krpc.client.services.SpaceCenter;
import krpc.client.services.SpaceCenter.Flight;
import krpc.client.services.SpaceCenter.ReferenceFrame;
import krpc.client.services.SpaceCenter.Vessel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SpaceShip {

    private final Connection krpcConnection;

    private final SpaceCenter spaceCenter;

    private final Vessel activeVessel;

    private Flight flightParameters;

    private ReferenceFrame pontoRefOrbital;

    private ReferenceFrame pontoRefSuperficie;

    private Stream<Double> meanAltitude;

    private Stream<Double> surfaceAltitude;

    private Stream<Double> apoapsisAltitude;

    private Stream<Double> periapsisAltitude;

    private Stream<Double> verticalSpeed;

    private Stream<Double> missionElapsedTime;

    private Stream<Double> horizontalSpeed;

    private Stream<Float> totalShipMass;

    private Stream<Float> electricCharge;

    private float totalElectricCharge;

    private float surfaceGravity;

    private String astronomicalObjectName;

    private int batteryCharge;

    public SpaceShip(Connection krpcConnection) throws RPCException {
        this.krpcConnection = krpcConnection;
        this.spaceCenter = SpaceCenter.newInstance(krpcConnection);
        this.activeVessel = this.spaceCenter.getActiveVessel();
        this.iniciarStreams();
    }

    private void iniciarStreams() {
        try {
            this.pontoRefOrbital = this.activeVessel.getOrbit().getBody().getReferenceFrame();
            this.pontoRefSuperficie = this.activeVessel.getSurfaceReferenceFrame();
            this.flightParameters = this.activeVessel.flight(pontoRefOrbital);
            this.meanAltitude = this.krpcConnection.addStream(this.flightParameters, "getMeanAltitude");
            this.surfaceAltitude = this.krpcConnection.addStream(this.flightParameters, "getSurfaceAltitude");
            this.apoapsisAltitude = this.krpcConnection.addStream(this.activeVessel.getOrbit(), "getApoapsisAltitude");
            this.periapsisAltitude = this.krpcConnection.addStream(this.activeVessel.getOrbit(), "getPeriapsisAltitude");
            this.verticalSpeed = this.krpcConnection.addStream(this.flightParameters, "getVerticalSpeed");
            this.horizontalSpeed = this.krpcConnection.addStream(this.flightParameters, "getHorizontalSpeed");
            this.totalShipMass = this.krpcConnection.addStream(this.activeVessel, "getMass");
            this.missionElapsedTime = this.krpcConnection.addStream(this.activeVessel, "getMET");
            this.electricCharge = this.krpcConnection.addStream(this.activeVessel.getResources(), "amount", "ElectricCharge");
            this.totalElectricCharge = this.activeVessel.getResources().max("ElectricCharge");
            this.surfaceGravity = this.activeVessel.getOrbit().getBody().getSurfaceGravity();
            this.astronomicalObjectName = this.activeVessel.getOrbit().getBody().getName();
        } catch (StreamException | RPCException | NullPointerException | IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "SpaceShip{" +
            "krpcConnection=" + krpcConnection +
            ", spaceCenter=" + spaceCenter +
            ", activeVessel=" + activeVessel +
            ", flightParameters=" + flightParameters +
            ", pontoRefOrbital=" + pontoRefOrbital +
            ", pontoRefSuperficie=" + pontoRefSuperficie +
            ", meanAltitude=" + meanAltitude +
            ", surfaceAltitude=" + surfaceAltitude +
            ", apoapsisAltitude=" + apoapsisAltitude +
            ", periapsisAltitude=" + periapsisAltitude +
            ", verticalSpeed=" + verticalSpeed +
            ", missionElapsedTime=" + missionElapsedTime +
            ", horizontalSpeed=" + horizontalSpeed +
            ", totalShipMass=" + totalShipMass +
            ", electricCharge=" + electricCharge +
            ", totalElectricCharge=" + totalElectricCharge +
            ", surfaceGravity=" + surfaceGravity +
            ", astronomicalObjectName='" + astronomicalObjectName + '\'' +
            ", batteryCharge=" + batteryCharge +
            '}';
    }
}
