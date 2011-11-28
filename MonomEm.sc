//-----------------------------------------------------------------
//
// Visual emulator for the Monome 40h.
// Sends OSC messages to the specified host/port in the same
// format as the 40h, and responds to LED actions.
//  __ Daniel Jones
//     http://www.erase.net
//
// Usage:
//    e = MonomEm.new(host, port);
//    m = Monome.new(host, port);
//    m.action = { |x, y, on| [x, y, on].postln };
//    m.led(5, 6, 1);
//
// or:
//    m = Monome.emu;
//    m.led(6, 7, 1);
//
//-----------------------------------------------------------------


MonomEm
{
 
 	var <host, <port;
 	var <width, <height;
	var window, matrix;
	var buttonsize = 16;
	var keymap;
	var addr, target;
	var resps;
	
	*new { |host = "127.0.0.1", port = 8080, width = 8, height = 8|
		^super.newCopyArgs(host, port, width, height).init;
	}
	
	init {
		window = Window.new("40h", Rect(30, 50, buttonsize * (width * 2 + 1), buttonsize * (height * 2 + 1)), false, true);
		height.do({ |y|
			matrix = matrix.add([]);
			width.do({ |x|
				var button;
				button = Button(window, Rect(buttonsize + (x * buttonsize * 2), buttonsize + (y * buttonsize * 2), buttonsize, buttonsize));
				button.states = [
					[ "", Color.gray, Color.gray ],
					[ "", Color.green, Color.green ]
				];

				button.mouseDownAction = { |button|
					this.press(x, y, 1);
				};
				button.mouseUpAction = { |button|
					this.press(x, y, 0);
				};

				button.action = { button.value = 1 - button.value; };
				matrix[y] = matrix[y].add(button);
			});
		});
		
		keymap = [
	      [ $1, $2, $3, $4, $5, $6, $7, $8 ],
	      [ $q, $w, $e, $r, $t, $y, $u, $i ],
	      [ $a, $s, $d, $f, $g, $h, $j, $k ],
	      [ $z, $x, $c, $v, $b, $n, $m, $, ],
	      [ $!, $@ ,$#, $$, $%, $^, $&, $* ],
	      [ $Q, $W, $E, $R, $T, $Y, $U, $I ],
	      [ $A, $S, $D, $F, $G, $H, $J, $K ],
	      [ $Z, $X, $C, $V, $B, $N, $M, $< ],
	    ];

		window.view.keyDownAction = { |view, char, modifiers, unicode, keycode|
			keymap.do({ |row, y|
				if (row.indexOf(char).notNil)
					{ this.press(row.indexOf(char), y, 1); };
			});
		};
		window.view.keyUpAction = { |view, char, modifiers, unicode, keycode|
			keymap.do({ |row, y|
				if (row.indexOf(char).notNil)
					{ this.press(row.indexOf(char), y, 0); };
			});
		};
		window.onClose = {
			resps.do({ |resp|
				OSCresponder.remove(resp);
			});
		};
		
		CmdPeriod.add({ window.close; });
		
		window.front;
		
		addr = NetAddr(host, port);
		resps = [];
		resps = resps.add(OSCresponder(addr, "/box/led", { |r, t, msg| this.led(msg[1], msg[2], msg[3]); }).add);
		resps = resps.add(OSCresponder(addr, "/box/led_row", { |r, t, msg| this.led_row(msg[1], msg[2], msg[3] ?? 0); }).add);
		resps = resps.add(OSCresponder(addr, "/box/led_col", { |r, t, msg| this.led_col(msg[1], msg[2], msg[3] ?? 0); }).add);
		
		target = NetAddr("127.0.0.1", port);
	}

	close {
		window.close;
	}
	
	press { |x, y, on|
		target.sendMsg("/box/press", x, y, on);
	}
	
	led { |x, y, on|
		postln("led " ++ [ x, y ] ++ ": " ++ on);
		defer({ matrix[y][x].value = on; });
	}
	
	led_row { |y, on, on2 = 0|
		on = on + (on2 * 256);
		defer({
			width.do({ |x|
				var pow = (2 ** x).asInteger;
				matrix[y][x].value = ((pow & on) > 0).binaryValue
			});
		});
	}
	
	led_col { |x, on, on2 = 0|
		on = on + (on2 * 256);
		defer({
			height.do({ |y|
				var pow = (2 ** y).asInteger;
				matrix[y][x].value = ((pow & on) > 0).binaryValue
			});
		});
	}
}

