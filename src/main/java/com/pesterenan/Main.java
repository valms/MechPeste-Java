package com.pesterenan;

import com.pesterenan.model.SpaceShip;
import com.pesterenan.service.LiftoffService;
import com.pesterenan.views.TestUI;
import java.io.IOException;
import krpc.client.Connection;
import krpc.client.RPCException;
import krpc.client.StreamException;

public class Main {

    public static void main(String[] args) {
        new TestUI();

        try (Connection connection = Connection.newInstance("TEST")) {
            SpaceShip spaceShip = new SpaceShip(connection);
            LiftoffService liftoffService = new LiftoffService(spaceShip);
            liftoffService.executeLiftoffProcedure();
            liftoffService.gravityCurve();

        } catch (IOException | RPCException | InterruptedException | StreamException e) {
            throw new RuntimeException(e);
        }

    }

}
