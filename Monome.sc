//-----------------------------------------------------------------
//
// Clean interactions with the Monome 40h.
//  __ Daniel Jones
//     http://www.erase.net
//
// Thanks to Tristan Strange for 128/256 support and custom prefix.
//  
// Run serialio with:
//    ./serialio localhost:57120
//
// Usage:
//    m = Monome.new(host, port, prefix, height, width);
//    m = Monome.new;
//    m.action = { |x, y, on|
//      [x, y, on].postln;
//    };
//    m.led(5, 6, 1);
//    m.led_row(4, 255);
//    m.intensity(0.5);
//    m.clear;
//
//-----------------------------------------------------------------

Monome
{
	var <host, <port;
	var <prefix;
	var <height, <width;
	var <>osc, <>target;
	var <>action;

	
	var <pressed;

	*new { |host = "127.0.0.1", port = 8080, prefix = "/box", height = 8, width = 8|
		^super.newCopyArgs(host, port, prefix, height, width).init;
	}
	
	*emu { |port = 57120, width = 8, height = 8|
		// spawn emulator
		MonomEm.new(port: port, width: width, height: height);
		^this.new(port: port, width: width, height: height).init;
	}
	
	init {
		pressed = [];
		height.do({ pressed = pressed.add(Array.fill(width, 0)) });		
		osc = OSCresponder.new(nil, prefix ++ "/press", { |time, resp, msg|
			pressed[msg[2]][msg[1]] = msg[3];
			if (action.notNil)
			   { action.value(msg[1], msg[2], msg[3]); };
		});
		osc.add;

		target = NetAddr(host, port);
	}

	setPrefix { |pre, dev|
		prefix = pre;
		(dev.notNil && pre.notNil).if(
			{ target.sendMsg("/sys/prefix", dev, prefix) },
			{ target.sendMsg("/sys/prefix", prefix) }
		);
	}

	led { |x, y, on = 1|
		target.sendMsg(prefix ++ "/led", x.asInteger, y.asInteger, on.asInteger);
	}

	led_row { |y, on = 255, on2|
		if (on2.isNil)
		{
			target.sendMsg(prefix ++ "/led_row", y.asInteger, on.asInteger);
		}
		{
			target.sendMsg(prefix ++ "/led_row", y.asInteger, on.asInteger, on2.asInteger);
		};
	}

	led_col { |x, on = 255, on2|
		if (on2.isNil)
		{
			target.sendMsg(prefix ++ "/led_col", x.asInteger, on.asInteger);
		}
		{
			target.sendMsg(prefix ++ "/led_col", x.asInteger, on.asInteger, on2.asInteger); 
		};
	}
	
	intensity { |i|
		target.sendMsg(prefix ++ "/intensity", i);
	}

	clear { |on|
		on = on ?? 0;
		width.do { |i| this.led_col(i, on * 255, on * 255) };
	}
	
	test { |on|
		target.sendMsg(prefix ++ "/test", on.asInteger);
	}
}

