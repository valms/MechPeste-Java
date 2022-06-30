package com.pesterenan.model;

import krpc.client.Connection;
import krpc.client.RPCException;
import krpc.client.Stream;
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

    private Stream<Double> altitude;

    private Stream<Double> surfaceAltitude;

    private Stream<Double> apoastro;

    private Stream<Double> periastro;

    private Stream<Double> velVertical;

    private Stream<Double> tempoMissao;

    private Stream<Double> velHorizontal;

    private Stream<Float> massaTotal;

    private Stream<Float> bateriaAtual;

    private float bateriaTotal;

    private float acelGravidade;

    private String corpoCeleste;

    private int batteryCharge;

    public SpaceShip(SpaceCenter spaceCenter, Connection krpcConnection) throws RPCException {
        this.krpcConnection = krpcConnection;
        this.spaceCenter = spaceCenter;
        this.activeVessel = spaceCenter.getActiveVessel();
    }





}
