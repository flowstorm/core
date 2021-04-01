#!/bin/bash

# Determine if install.stage exists
if [ -f $HOME/install.stage ]; then
    # Extract the last line out of the file
    cd $HOME
    last_stage=$(head -n 1 install.stage)

    # Check if last_stage is set, that is the next command has to be executed.
    if [ ! -z ${last_stage} ]; then
        # Execute the next stage.
        if [ 1 = ${last_stage} ]; then
              echo "Second stage"
              cd $HOME
              sudo chmod +x $HOME/GassistPi/audio-drivers/AIY-HAT/scripts/install-alsa-config.sh
              sudo $HOME/GassistPi/audio-drivers/AIY-HAT/scripts/install-alsa-config.sh
              cd $HOME

              echo "Second install stage proceeded"
        else
              echo "Everything is set. You can run speaker-test now. Be aware that it can be loud."
        fi
    fi
else
   # File does not exist - start from beginning
    echo "First stage (after automatic reboot, please run $0 again)"

    echo "Set device hostname = unique identification (don't forget to register into databases!)"
    read hostname && echo $hostname > /etc/hostname

    cd $HOME
    mkdir .ssh
    # adding deployer service account public key
    echo "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDDhJkp5oOUx0FFfMlZQv0T852uUCCCP+432Tlb4eBGmNUI+KppocLrIjVOUiTqYaiPAH4Li8QE42kldN6zQB/dXd4IzKGrezVAQ2IkpWZS7Pfhksga+HfhTh40LdQf7GGthLXdKCKeTDEvk47udjNAXs8yYzoq3n9olZpUSgbdoQD7/No4rapL1VdmN+SOEVPrHnrPntPcm4kzRKGHKjFMbFUDhQoUYk3d/qj6donvtq+phrlwM/W3IYujpTvKfMWNCqFoMxE7viwGr0IBbTEBSPq8QJPphVLcBXm6Ks8w31mPoL1OErrdTLLBk9WtAcrfLBycWAmUc4hbTCUv6wgD deployer@jump.promethist.ai" >> .ssh/authorized_keys

    sudo apt-get -y update
    sudo apt-get install -y openvpn
    #sudo apt-get install -y python3
    #sudo apt-get install -y python3-pip
    sudo apt-get install -y g++
    sudo apt-get install -y gcc
    sudo apt-get install -y libasound-dev
    sudo apt-get install -y portaudio19-dev
    sudo apt-get install -y rpi.gpio
    sudo apt-get install -y mpg321
    sudo apt-get install -y flac
    wget https://repository.promethist.ai/dist/promethist.jar
    wget https://repository.promethist.ai/dist/jdk-8u231-linux-arm32-vfp-hflt.tar.gz
    tar xvfz jdk*.tar.gz
    ln -s jdk1.8.0_231 jdk8

    sudo bash -c 'echo "[Unit]
Description=OpenVPN service connects device to jump.promethist.ai server for allowing remote support access
After=sound.target network.target
Wants=sound.target

[Service]
ExecStart=/usr/sbin/openvpn /home/pi/$1.ovpn
WorkingDirectory=/home/pi
Restart=always
User=root
PrivateTmp=true

[Install]
Alias=ovpn
WantedBy=multi-user.target" > /etc/systemd/system/openvpn.service' -- "`hostname`"

    cd $HOME
    git clone https://github.com/shivasiddharth/GassistPi
    sudo chmod +x $HOME/GassistPi/audio-drivers/AIY-HAT/scripts/configure-driver.sh
    sudo $HOME/GassistPi/audio-drivers/AIY-HAT/scripts/configure-driver.sh

    sudo bash -c 'echo "[Unit]
Description=Promethist desktop client run script
After=sound.target network.target
Wants=sound.target

[Service]
TimeoutSec=0
ExecStart=/home/pi/flowstorm.run bg
WorkingDirectory=/home/pi
User=pi
Restart=always
PrivateTmp=true

[Install]
Alias=promethist
WantedBy=multi-user.target" > /etc/systemd/system/promethist.service'

    sudo systemctl daemon-reload
    sudo systemctl enable ovpn.service
    sudo systemctl enable promethist.service

    echo "1" > $HOME/install.stage
    read -n 1 -s -r -p "Press any key to restart device."

    sudo reboot
fi




