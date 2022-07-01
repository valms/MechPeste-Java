package com.pesterenan;

import com.pesterenan.model.SpaceShip;
import com.pesterenan.service.LiftoffService;
import java.io.IOException;
import krpc.client.Connection;
import krpc.client.RPCException;

public class Main {

    public static void main(String[] args) {

        try (Connection connection = Connection.newInstance("TEST")) {
            SpaceShip spaceShip = new SpaceShip(connection);
            LiftoffService liftoffService = new LiftoffService(spaceShip);
            liftoffService.executeLiftoffProcedure();

        } catch (IOException | RPCException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

}
