package cannon;

import app.StartDesktopApp;
import manager.ai.AIRegistry;

/**
 * The main method of this launches the Ludii application with its GUI, and registers
 * the example AIs from this project such that they are available inside the GUI.
 *
 * @author Dennis Soemers
 */
public class Main
{

    /**
     * The main method
     * @param args
     */
    public static void main(final String[] args)
    {
        // Register our example AIs
        AIRegistry.registerAI("Example Random AI", () -> {return new RandomAI();}, (game) -> {return true;});

        // Run Ludii
        StartDesktopApp.main(new String[0]);
    }

}