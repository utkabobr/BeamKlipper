package ru.ytkab0bp.beamklipper;

public enum InstanceIcon {
    PRINTER(R.drawable.ic_printer_outline_28),
    GRID(R.drawable.ic_grid_layout_outline_28),
    BOX(R.drawable.ic_cube_box_outline_28),
    HOME(R.drawable.ic_home_outline_28),
    GAME(R.drawable.ic_game_outline_28),
    MAGIC_HAT(R.drawable.ic_magic_hat_outline_28),
    MOON(R.drawable.ic_moon_outline_28),
    INBOX(R.drawable.ic_inbox_outline_28),
    LOCATION(R.drawable.ic_location_outline_28),
    ROBOT(R.drawable.ic_robot_outline_28),
    SERVICES(R.drawable.ic_services_outline_28),
    SHOPPING_CART(R.drawable.ic_shopping_cart_outline_28),
    TRUCK(R.drawable.ic_truck_outline_28),
    SNEAKER(R.drawable.ic_sneaker_outline_28);

    public final int drawable;

    InstanceIcon(int drawable) {
        this.drawable = drawable;
    }

    public static InstanceIcon byKey(String key) {
        for (InstanceIcon i : values()) {
            if (i.name().equals(key)) {
                return i;
            }
        }
        return InstanceIcon.PRINTER;
    }
}
