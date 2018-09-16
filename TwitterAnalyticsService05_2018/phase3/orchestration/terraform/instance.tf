###########################################################################
# Front Instance Template for Query1.                                     #
###########################################################################

# pre-set common_tags
locals {
  common_tags = {
    Project = "Phase3"
  }
}

provider "aws" {
  region = "us-east-1"
}

# create security_groups
resource "aws_security_group" "sg" {
  name = "sg"
  # Allow all
  ingress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Allow all
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = "${local.common_tags}"
}

# create target group
resource "aws_lb_target_group" "target_group" {
  name     = "myTargetGroup"
  port     = 80
  protocol = "HTTP"
  vpc_id   = ""
  health_check{
  path = "/"
  protocol ="HTTP"
  port = 80
  }
}

# create target group
resource "aws_lb" "lb" {
  name            = "lb"
  internal        = false
  security_groups = ["team"]
  subnets         = ["",""]
  tags  = "${local.common_tags}"
}

# create 4 front instances
resource "aws_spot_instance_request" "instance1" {
  ami             = "ami-43a15f3e"
  instance_type   = "m5.large"
  security_groups = ["team"]
  spot_price    = "0.04"
  availability_zone = "us-east-1c"
  tags = "${local.common_tags}"
  key_name = "privateKey"
}

resource "aws_spot_instance_request" "instance2" {
  ami             = "ami-43a15f3e"
  instance_type   = "m5.large"
  security_groups = ["team"]
  spot_price    = "0.04"
  availability_zone = "us-east-1c"
  tags = "${local.common_tags}"
  key_name = "privateKey"
}

resource "aws_spot_instance_request" "instance3" {
  ami             = "ami-43a15f3e"
  instance_type   = "m5.large"
  security_groups = ["team"]
  spot_price    = "0.04"
  availability_zone = "us-east-1c"
  tags = "${local.common_tags}"
  key_name = "privateKey"
}

resource "aws_spot_instance_request" "instance4" {
  ami             = "ami-43a15f3e"
  instance_type   = "m5.large"
  security_groups = ["team"]
  spot_price    = "0.04"
  availability_zone = "us-east-1c"
  tags = "${local.common_tags}"
  key_name = "privateKey"
}
