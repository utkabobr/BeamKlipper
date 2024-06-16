// Very basic support via a Linux gpiod device
//
// Copyright (C) 2017-2021  Kevin O'Connor <kevin@koconnor.net>
//
// This file may be distributed under the terms of the GNU GPLv3 license.

#include <fcntl.h> // open
#include <stdio.h> // snprintf
#include <stdlib.h> // atexit
#include <string.h> // memset
#include <sys/ioctl.h> // ioctl
#include <unistd.h> // close
#include <linux/gpio.h> // GPIOHANDLE_REQUEST_OUTPUT
#include "command.h" // shutdown
#include "gpio.h" // gpio_out_write
#include "internal.h" // report_errno
#include "sched.h" // sched_shutdown

#define GPIO_CONSUMER "klipper"

DECL_ENUMERATION_RANGE("pin", "gpio0", GPIO(0, 0), MAX_GPIO_LINES);
DECL_ENUMERATION_RANGE("pin", "gpiochip0/gpio0", GPIO(0, 0), MAX_GPIO_LINES);
DECL_ENUMERATION_RANGE("pin", "gpiochip1/gpio0", GPIO(1, 0), MAX_GPIO_LINES);
DECL_ENUMERATION_RANGE("pin", "gpiochip2/gpio0", GPIO(2, 0), MAX_GPIO_LINES);
DECL_ENUMERATION_RANGE("pin", "gpiochip3/gpio0", GPIO(3, 0), MAX_GPIO_LINES);
DECL_ENUMERATION_RANGE("pin", "gpiochip4/gpio0", GPIO(4, 0), MAX_GPIO_LINES);
DECL_ENUMERATION_RANGE("pin", "gpiochip5/gpio0", GPIO(5, 0), MAX_GPIO_LINES);
DECL_ENUMERATION_RANGE("pin", "gpiochip6/gpio0", GPIO(6, 0), MAX_GPIO_LINES);
DECL_ENUMERATION_RANGE("pin", "gpiochip7/gpio0", GPIO(7, 0), MAX_GPIO_LINES);
DECL_ENUMERATION_RANGE("pin", "gpiochip8/gpio0", GPIO(8, 0), MAX_GPIO_LINES);

struct gpio_line {
    int chipid;
    int offset;
    int fd;
    int state;
};

struct gpio_out gpio_out_setup(uint32_t pin, uint8_t val) {
    return (struct gpio_out){
            .line=&(struct gpio_line) {
                .chipid = 0,
                .state = 0
            }
    };
}
void gpio_out_reset(struct gpio_out g, uint8_t val) {
}
void gpio_out_toggle_noirq(struct gpio_out g) {
}
void gpio_out_toggle(struct gpio_out g) {
}
void gpio_out_write(struct gpio_out g, uint8_t val) {
}
struct gpio_in gpio_in_setup(uint32_t pin, int8_t pull_up) {
    return (struct gpio_in){
        .line=&(struct gpio_line) {
            .chipid = 0,
            .state = 0
        }
    };
}
void gpio_in_reset(struct gpio_in g, int8_t pull_up) {
}
uint8_t gpio_in_read(struct gpio_in g) {
    return 0;
}
struct gpio_pwm gpio_pwm_setup(uint32_t pin, uint32_t cycle_time, uint16_t val) {
    return (struct gpio_pwm){};
}
void gpio_pwm_write(struct gpio_pwm g, uint16_t val) {
}
struct gpio_adc gpio_adc_setup(uint32_t pin) {
    return (struct gpio_adc){};
}
uint32_t gpio_adc_sample(struct gpio_adc g) {
    return 0;
}
uint16_t gpio_adc_read(struct gpio_adc g) {
    return 0;
}
void gpio_adc_cancel_sample(struct gpio_adc g) {
}

struct spi_config
spi_setup(uint32_t bus, uint8_t mode, uint32_t rate)
{
    return (struct spi_config){ };
}
void
spi_prepare(struct spi_config config)
{
}
void
spi_transfer(struct spi_config config, uint8_t receive_data
        , uint8_t len, uint8_t *data)
{
}