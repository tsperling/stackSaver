Graphics3D 640,480,16,0
;Graphics3D 1024,768,32,0
SetBuffer BackBuffer()
SeedRnd (MilliSecs()) ;Seed the random number generator

ClearTextureFilters

;MusicLoop=LoadSound("music.mp3")
;LoopSound MusicLoop
;PlaySound MusicLoop

gunfx=LoadSound("bullet.wav")
bombfx=LoadSound("bomb.wav")
upfx=LoadSound("up.wav") : downfx=LoadSound("down.wav")
diefx=LoadSound("die.wav")
spawnfx=LoadSound("spawn.wav")
shieldfx=LoadSound("shield.wav")
addbombfx=LoadSound("addbomb.wav")

;Type declarations
;For the FPS limiter
Type FrameRate
	Field TargetFPS#	
	Field SpeedFactor#
	Field FPS#	
	Field TicksPerSecond	
	Field CurrentTicks	
	Field FrameDelay	
End Type
Global FL.FrameRate = New FrameRate
;For the bullets
Type bullet
	Field sprite
	Field x#,z#,angle#
	Field ingame
End Type
;For the secondary muzzle flash (basically the same as a bullet)
Type flash2
	Field sprite
	Field x#,z#,angle#
	Field ingame
End Type
;For the shockwave(s) (again, basically the same as a bullet)
Type shock
	Field sprite
	Field x#,z#,y#,scale#
	Field ingame
End Type
;For the shockwaves created by the player forcefield
Type shockfield
	Field sprite
	Field x#,z#,y#,scale#
	Field ingame
End Type
;For the explosions that occur when an enemy is destroyed
Type explode
	Field sprite
	Field x#,z#,y#,scale#
	Field ingame
End Type
;And finally, for enemies
Type endrone
	Field sprite
	Field x#,z# ;X, Z, TarX and TarZ don't actually represent the postion of the enemy - more the position that the enemy is headed to
	Field tarx#,tarz#
	Field ingame ;Is the enemy actually in the game?
	Field energy ;Energy level for enemy (Not actually used, but retained just in case)
	Field level ;The level that the enemy is on
	Field action ;Action 0 = Acting Normally (free roaming), 1 = Chasing the player
	Field ID
End Type

Dim levlife#(4)

;Resolution stuff
graphw=GraphicsWidth()
graphh=GraphicsHeight()
;Move mouse into center of screen (0,0). You can alter the '+10' to decide which way the player faces at start.
MoveMouse graphw/2,graphh/2-10

camera=CreateCamera()
;RotateEntity camera,40,0,0:PositionEntity camera,0,3.5,-4 ;Rotate and postion camera
RotateEntity camera,40,0,0:PositionEntity camera,0,4,-6 ;Rotate and postion camera

;light=CreateLight()
;RotateEntity light,90,0,0

; Create character Poly
poly=CreatePoly(-0.5,0.5,1,1) : PositionEntity poly,0,0.5,0 : EntityColor poly,0,255,0 : EntityFX poly,1
EntityType poly,1 ; Define main character collision detection

; Gun object
gun=CreateCylinder(8,False) : ScaleEntity gun,0.2,0.3,0.2 : PositionEntity gun,0,1,-0.2 : EntityFX gun,1
gunend=CreateBullet(0,0,0.4,0.4) : RotateEntity gunend,0,0,180 : PositionEntity gunend,0.2,0.7,0 : EntityFX gunend,1
gunmiddle=CreateCylinder(8,False) : ScaleEntity gunmiddle,0.18,0.25,0.18 : PositionEntity gunmiddle,0,1,-0.2 : EntityFX gunmiddle,1
flash=BuildMuzzleFlash() : ScaleEntity flash,0.5,0.5,0.5 : RotateEntity flash,-90,0,0 : PositionEntity flash,0,1.3,-0.2 : EntityFX flash,1

EntityParent gun,poly : EntityParent gunend,gun : EntityParent gunmiddle,flash : EntityParent flash,poly

crosshair=CreatePoly(-0.25,0.25,0.5,0.5) : EntityColor crosshair,255,0,0
;crosshair=CreatePoly(-0.25,0.75,0.5,1) : EntityColor crosshair,255,0,0
HideEntity crosshair

;shadow=CreatePoly(-0.5,1,1,1) : EntityColor shadow,0,0,0

leveltex=LoadAnimTexture("levels.png",1,200,200,0,4)

;Create the floors
backgrnd=CreatePoly(-10,10,20,20) : PositionEntity backgrnd,0,-0.1,0 : EntityColor backgrnd,255,255,255 : EntityTexture backgrnd,leveltex,0 : EntityFX backgrnd,1
backgrnd2=CreatePoly(-10,10,20,20) : PositionEntity backgrnd2,0,5.1,0 : EntityColor backgrnd2,255,255,255 : EntityTexture backgrnd2,leveltex,1 : EntityFX backgrnd2,1
backgrnd3=CreatePoly(-10,10,20,20) : PositionEntity backgrnd3,0,10.1,0 : EntityColor backgrnd3,255,255,255 : EntityTexture backgrnd3,leveltex,2 : EntityFX backgrnd3,1
backgrnd4=CreatePoly(-10,10,20,20) : PositionEntity backgrnd4,0,15.1,0 : EntityColor backgrnd4,255,255,255 : EntityTexture backgrnd4,leveltex,3 : EntityFX backgrnd4,1
EntityParent backgrnd2,backgrnd : EntityParent backgrnd3,backgrnd : EntityParent backgrnd4,backgrnd

;Create enemies before game starts and place in 'holding pen' (offscreen)
;400 drones (100 per level) - known ingame as enemy type 1
For i=0 To 399
	e.endrone=New endrone
	e\sprite=CreatePoly(-0.5,0.5,1,1) : PositionEntity e\sprite,0,100,0
	EntityFX e\sprite,1
	EntityParent e\sprite,backgrnd
	EntityColor e\sprite,255,0,0
	EntityType e\sprite,2 : EntityRadius e\sprite,0.85; Enemy collision detection
	e\energy=100
	e\level=0
	e\ingame=False
	e\ID=i
Next
enemytarget=CreateBullet(-0.25,0.25,0.5,0.5) ; The object that we use to make each enemy face a certain way
EntityParent enemytarget,backgrnd : HideEntity enemytarget

;Load bullet and flash (not muzzle flash) texture
bullettex=LoadTexture("bullet.png",4) : flash2text=LoadTexture("flash2.tga",3) : shocktext=LoadTexture("shock.png",4) : shock2text=LoadTexture("shock2.png",4)

;Do the same with 50 bullets - pre-create and hide from view
For i=0 To 49
	bt.bullet=New bullet
	bt\sprite=CreateBullet(-0.25,0.5,0.5,1) : PositionEntity bt\sprite,0,100,0
	EntityColor bt\sprite,255,255,255
	EntityAlpha bt\sprite,0.9
	EntityType bt\sprite,3 : EntityRadius bt\sprite,0.1 ;The bullet uses spherical collision detection
	EntityTexture bt\sprite,bullettex
	EntityFX bt\sprite,1
	HideEntity bt\sprite
	bt\ingame=False
Next
;And the same with 5 shooting flashes - pre-create and hide from view
For i=0 To 4
	f2.flash2=New flash2
	f2\sprite=CreateFlash() : PositionEntity f2\sprite,0,100,0
	EntityColor f2\sprite,255,255,255
	EntityTexture f2\sprite,flash2text
	EntityFX f2\sprite,1
	HideEntity f2\sprite
	f2\ingame=False
Next
;And, yes... the same with 20 shockwave objects
For i=0 To 19
	sh.shock=New shock
	sh\sprite=CreateBullet(-0.5,0.5,1,1) : PositionEntity sh\sprite,0,100,0
	EntityColor sh\sprite,255,255,255
	EntityTexture sh\sprite,shocktext
	EntityFX sh\sprite,1
	HideEntity sh\sprite
	sh\ingame=False
	sh\scale#=1
Next
;... the same with 10 player shockwave objects
For i=0 To 9
	sf.shockfield=New shockfield
	sf\sprite=CreateBullet(-0.5,0.5,1,1) : PositionEntity sf\sprite,0,100,0
	EntityColor sf\sprite,255,255,255
	EntityTexture sf\sprite,shock2text
	EntityFX sf\sprite,1
	HideEntity sf\sprite
	sf\ingame=False
	sf\scale#=1
Next
;And last, but not least, let's create 10 explosion objects
For i=0 To 9
	ex.explode=New explode
	ex\sprite=CreateFlash() : PositionEntity ex\sprite,0,100,0
	EntityColor ex\sprite,255,0,0
	;EntityTexture ex\sprite,explodetext
	EntityFX ex\sprite,1
	HideEntity ex\sprite
	ex\ingame=False
	ex\scale#=1
Next

;Load the 'av.bmp' combined avatar graphic as a texture
;avcount=50
;avtex=LoadAnimTexture("av.png",1,60,60,0,avcount)
guntex=LoadTexture("gun.png",4) : gunbacktex=LoadTexture("gunback.png",4) : gunmiddletex=LoadTexture("gunmiddle.png",1)
flashtext=LoadAnimTexture("flash.tga",3,100,100,0,2)
titletext=LoadTexture("title.png",1)

;Texture Poly with random avatar image
;randtex=Rand(0,avcount-1)
;EntityTexture poly,avtex,randtex
EntityTexture gun,guntex : EntityTexture gunend,gunbacktex : EntityTexture gunmiddle,gunmiddletex
EntityTexture flash,flashtext,1

Collisions 3,2,2,1 ;(bullet) to (enemy) collision testing

titlescreen=CreateSprite() : EntityTexture titlescreen,titletext : EntityOrder titlescreen,-1
PositionEntity titlescreen,0,1.5,-3
ScaleSprite titlescreen,4,3
EntityParent titlescreen,camera

;Spawn an enemy (test)
;SpawnEnemy(0,8,1,0,1)

;-------------------
;Title Screen Loop  --------------
;-------------------
.titlescreen
ShowEntity titlescreen

Repeat
If KeyHit(1) Then Goto endgame
If KeyHit(28)
	gameover=False
	HideEntity titlescreen
	Goto startgame
EndIf
RenderWorld
Color 240,240,240
Text graphw/2,graphh/2+20,"To Start, press ENTER, or ESC to exit",True
Flip
Forever

;-------------------
;Main Game Loop     --------------
;-------------------

.startgame

;Setting some game variables
FrameLimitInit(35.0)
millifire#=MilliSecs()
counter1#=MilliSecs()
tripleguntime#=MilliSecs()
gametime=0
playerdead=0
playerlevel=1
starttriplegun=0
playerscore=0
playerprotect=0
playerprotectreset=0
nukescore=0
nukecount=2
secs=0
levlife#(1)=100:levlife#(2)=100:levlife#(3)=100:levlife#(4)=100
For ene.endrone=Each endrone
	PositionEntity ene\sprite,0,100,0
	ene\energy=100
	ene\level=0
	ene\ingame=False
Next
MoveMouse graphw/2,graphh/2-10
PositionEntity backgrnd,0,-0.1,0

;Game Loop starts
While Not gameover=True

Gosub SetSpeedFactor

Mousespeed=30
;Setting any variables that depend on the speed the game is running at
polyspeed#=0.1 * FL\SpeedFactor
enemyspeed#=0.03 * FL\SpeedFactor
bulletspeed#=0.6 * FL\SpeedFactor
flashspeed#=0.3 * FL\SpeedFactor
enemytarspeed#=0.05 * FL\SpeedFactor
enemyleveldamage#=0.001 * FL\SpeedFactor : playerlevelrepair#=0.004 * FL\SpeedFactor : levelselfrepair#=0.002 * FL\SpeedFactor
shockspeed#=0.25 * FL\SpeedFactor
explodespeed#=0.2 * FL\SpeedFactor

firerate#=100

If playerdead=1 ;If the player has been killed, restart the game
	If playerdeadtime#+2000<MilliSecs() ;If two seconds have passed since death
		playerprotect=1 : playerprotectreset=1 ;Kill any nearby enemies
		PositionEntity backgrnd,0,levely#(playerlevel),0 ;Move the background back to the start position
		MoveMouse graphw/2,graphh/2-10 ;Move the mouse so it's pointing forwards
		ShowEntity poly ;: ShowEntity crosshair : ShowEntity shadow 
		playerdead=0
		;Remove 1 life
		If gametime > 3 Then gametime=gametime-2;Decrease level difficulty by 1 (or two)
	EndIf
EndIf

If starttriplegun=1 ;If triplegun is activated (note - although it's called triplegun, it's not. It used to be, but it's now used for a temporary forcefield. Long story.)
	starttriplegun=1
	playerprotect=1
	If tripleguntime#+10000<MilliSecs() ;If ten seconds have passed, turn off the forcefield (don't have to do anything - it's turned off automatically)
		starttriplegun=0
		playerprotectreset=1
	EndIf
EndIf
;If the player is protected, create a shockwave around him (or some other effect)
If playerprotect=1
	If ChannelPlaying(shieldchan)=0 Then shieldchan=PlaySound(shieldfx)
	fire_shockfield(EntityX#(poly),EntityY#(poly)+0.2,EntityZ#(poly))
EndIf

;Mouse Control (providing the player is alive)
If playerdead=0
	mouseposx#=MouseX()-graphw/2
	mouseposz#=MouseY()-graphh/2
	tempx#=mouseposx#
	tempz#=mouseposz#
Else
	mouseposx#=tempx#
	mouseposz#=tempz#
EndIf

XAngle# = (mouseposx# - EntityX#(poly))
YAngle# = (mouseposz# - EntityZ#(poly))

mouseposx#=mouseposx#/mousespeed
mouseposz#=(mouseposz#/mousespeed)*-1

Angle# = ATan2 (YAngle#, XAngle#)
If Angle# < 0 Then Angle# = Angle# +360 ;Wrap angle value

RotateEntity poly,-90,(Angle#*-1)+90,0
;RotateEntity shadow,0,(Angle#*-1)+90,0
RotateEntity crosshair,0,(Angle#*-1)+90,0

TurnEntity flash,0,0,gunspinrate#

PositionEntity crosshair,(Sin(Angle#+90)*1.1)+EntityX#(poly),0,(Cos(Angle#+90)*1.1)+EntityZ#(poly)

;The problem is with the following line. We can't move the player from 0,0 ,or the circular mouse movement
;screws up... for some mathematical reason that I don't have time to figure out.
;Instead, we work around it... we'll move the camera and the background instead.
MoveMouse ((EntityX#(crosshair)*mousespeed)+graphw/2),(((EntityZ#(crosshair)*mousespeed)*-1)+graphh/2)

If gunflash=1
	EntityTexture flash,flashtext,0
Else
	EntityTexture flash,flashtext,1
EndIf

;Mouse buttons
If MouseDown(1)=True And playerdead=0 ;LMB
	If millifire#+firerate#<MilliSecs() ; If a bullet can be fired
		PlaySound gunfx
		fire_bullet(EntityX#(crosshair),EntityZ#(crosshair),Angle#) ; needs an 'x', a 'z' and an angle
		gunflash=1
		fire_flash(EntityX#(crosshair),EntityZ#(crosshair),Angle#) ; takes the same input as the bullet function
		millifire#=MilliSecs() ; Reset the millicount
		EntityTexture flash,flashtext,0
	Else
		gunflash=0
	EndIf
	If gunspinrate# < 7
		gunspinrate#=gunspinrate#+(0.2 * FL\SpeedFactor)
	EndIf
Else
	gunflash=0
	If gunspinrate# > 0
		gunspinrate#=gunspinrate#-(0.05 * FL\SpeedFactor)
	Else gunspinrate# = 0
	EndIf
EndIf
If MouseHit(2)=True And playerdead=0 ;RMB
	If nukecount > 0
		PlaySound bombfx
		screenflash=1
		fire_shockfield(EntityX#(poly),EntityY#(poly)+0.2,EntityZ#(poly)) ;Create 'shockwave'
		nukelevel=1
		nukecount=nukecount-1
		levlife#(playerlevel)=levlife#(playerlevel)+40 ;Heal the level a bit
		If levlife#(playerlevel) > 100 Then levlife#(playerlevel)=100
	EndIf
EndIf

; process bullets that have been created with fire_bullet (and that are actually in the game)
For b.bullet=Each bullet
	If b\ingame=True
	EntityParent b\sprite,backgrnd
	If EntityCollided(b\sprite,2) Then ;If the bullet has collided with an enemy object
		NameEntity EntityCollided(b\sprite,2),"DEAD";All we do to destroy an enemy is to name it "DEAD".
		HideEntity b\sprite : b\ingame=False : EntityParent b\sprite,0 ;Delete the bullet (in effect)
	ElseIf EntityDistance#(poly,b\sprite)>30 ;ElseIf the bullet's position reaches a certain point, delete it anyway
		HideEntity b\sprite : b\ingame=False : EntityParent b\sprite,0
	Else
		MoveEntity b\sprite,0,0,bulletspeed# ;Move the bullet forward
	EndIf
	EndIf ;If b\ingame=True
Next

; process shockwaves created with fire_shock
For sw.shock=Each shock
	EntityParent sw\sprite,backgrnd
	sw\scale#=sw\scale#+shockspeed#
	ScaleEntity sw\sprite,sw\scale#,1,sw\scale#
	TurnEntity sw\sprite,0,5,0
	EntityAlpha sw\sprite,((sw\scale#-6)*-1)/6
	If sw\scale# > 6
		HideEntity sw\sprite
		sw\ingame=False
		sw\scale#=1
	EndIf
Next

; and those created with fire_shockfield
For sfd.shockfield=Each shockfield
	sfd\scale#=sfd\scale#+shockspeed#
	ScaleEntity sfd\sprite,sfd\scale#,1,sfd\scale#
	TurnEntity sfd\sprite,0,5,0
	EntityAlpha sfd\sprite,((sfd\scale#-4)*-1)/4
	If sfd\scale# > 4
		HideEntity sfd\sprite
		sfd\ingame=False
		sfd\scale#=1
	EndIf
Next

; and the explosions created with fire_explode
For expl.explode=Each explode
	EntityParent expl\sprite,backgrnd
	ScaleSprite expl\sprite,expl\scale#,expl\scale#
	expl\scale#=expl\scale#+explodespeed#
	MoveEntity expl\sprite,0,explodespeed#,0
	EntityAlpha expl\sprite,((expl\scale#-3.5)*-1)/3.5
	If expl\scale# > 3.5
		HideEntity expl\sprite
		expl\ingame=False
		expl\scale#=1
	EndIf
Next

;Loop through every enemy entity (as long as they're on the same level as the player)
For z.endrone=Each endrone
If z\level=playerlevel ;So long as the enemy is on the same level as the player

;Destroys all enemies on the level (nuke)
If nukelevel=1 Then NameEntity z\sprite,"DEAD" ;Destroy enemy

;Loop that destroys all enemies within a certain distance of the player (used when moving through levels and coming back to life)
If EntityDistance(z\sprite,poly)<=3 And playerprotect=1
	NameEntity z\sprite,"DEAD" ;Destroy enemy
	playerprotectreset=1 ;Resest playerprotect variable (takes place after looping through enemies - see below)
ElseIf EntityDistance(z\sprite,poly)<=0.5 And playerprotect=0 And playerdead=0 ;If the enemy gets too close to the enemy, destroy the both
	NameEntity z\sprite,"DEAD" ;Destroy enemy
	ResetEntity poly
	screenflash=1
	HideEntity poly : HideEntity crosshair ;: HideEntity shadow
	playerdead=1 : playerdeadtime#=MilliSecs() ;kill player
EndIf ;If EntityDistance(z\sprite,poly)<2

If EntityDistance(z\sprite,flash)<0.6 Then ;If the muzzle flash gets close to an enemy, kill the enemy.
	If gunflash=1;If a bullet has been fired (detected by gunflash=1)
		NameEntity z\sprite,"DEAD" ;Destroy enemy
		;l.bullet=Last bullet : FreeEntity l\sprite : Delete l ;Destroy the last bullet created (unused caused problems)
	EndIf
EndIf

If EntityName$(z\sprite)="DEAD" And z\ingame=True ;Checks all enemies to see if they are called "DEAD"
	If ChannelPlaying(diechan1)=0 ;Statement that makes sure a maximum of 3 death sounds can play.
		diechan1=PlaySound(diefx)
		ChannelPitch diechan1, Rand(30000,50000)
	ElseIf ChannelPlaying(diechan2)=0
		diechan2=PlaySound(diefx)
		ChannelPitch diechan2, Rand(30000,50000)
	ElseIf ChannelPlaying(diechan3)=0
		diechan3=PlaySound(diefx)
		ChannelPitch diechan3, Rand(30000,50000)
	EndIf
	fire_shock(EntityX#(z\sprite),EntityY#(z\sprite),EntityZ#(z\sprite))
	fire_explode(EntityX#(z\sprite),EntityY#(z\sprite),EntityZ#(z\sprite))
	z\ingame=False
	z\level=0
	HideEntity z\sprite
	PositionEntity z\sprite,0,100,0
	NameEntity z\sprite,"100"
	playerscore=playerscore+10
	nukescore=nukescore+1
	triplescore=triplescore+1
	;triplescore=triplescore+1
	If nukescore > 200
		nukecount=nukecount+1 : nukescore=1
		PlaySound(addbombfx)
		playerscore=playerscore+20
	EndIf
	If triplescore > 100
		triplescore=1 : starttriplegun=1 : tripleguntime#=MilliSecs()
		playerscore=playerscore+5
	EndIf
EndIf
If playerdead=1 Then z\action=0
EndIf ;If z\level=playerlevel
Next

If playerprotectreset=1
	playerprotect=0
	playerprotectreset=0
EndIf

nukelevel=0 ;turn off nukelevel

;Keyboard Control (as long as player is still alive)
If playerdead=0
	If KeyDown(17) Then ;W
		If EntityZ#(backgrnd) > -9.8 Then TranslateEntity backgrnd,0,0,-1*polyspeed#
	End If
	If KeyDown(30) Then	;A
		If EntityX#(backgrnd) < 9.9 Then TranslateEntity backgrnd,polyspeed#,0,0
	End If
	If KeyDown(31) Then ;S
		If EntityZ#(backgrnd) < 9.8 Then TranslateEntity backgrnd,0,0,polyspeed#
	End If
	If KeyDown(32) Then ;D
		If EntityX#(backgrnd) > -9.9 Then TranslateEntity backgrnd,-1*polyspeed#,0,0
	End If
	If KeyHit(57) Then ;Spacebar
		;If player is in the right place, transport up (or down) to another level
		If EntityX#(backgrnd)<=-3.8 And EntityX#(backgrnd)>=-6.8 And EntityZ#(backgrnd)<=1.5 And EntityZ#(backgrnd)>=-1.5
			;Player in Right Hand transport area (up)
			PlaySound upfx
			If playerlevel < 4 Then playerlevel = playerlevel + 1 : Gosub changeplayerlevel
		ElseIf EntityX#(backgrnd)<=6.5 And EntityX#(backgrnd)>=3.5 And EntityZ#(backgrnd)<=1.5 And EntityZ#(backgrnd)>=-1.5
			;Player in Left Hand transport area (down)
			PlaySound downfx
			If playerlevel > 1 Then playerlevel = playerlevel - 1 : Gosub changeplayerlevel
		EndIf
	End If
EndIf

If KeyHit(1) ;The ESC button
	holdmouseX=MouseX() : holdmouseY=MouseY()
	Goto pause
EndIf
.resumegame

;Keys 1-4, purely for testing purposes (spawn an enemy on the specified level)
;If KeyHit(2) Then SpawnEnemy(0,8,1,0,1) : If KeyHit(3) Then SpawnEnemy(0,8,2,0,1) : If KeyHit(4) Then SpawnEnemy(0,8,3,0,1) : If KeyHit(5) Then SpawnEnemy(0,8,4,0,1)

PointEntity camera,crosshair

UpdateWorld
RenderWorld

;Drawing the minimap (with blocks for enemies and the player)
Color 0,255,0
Rect ((EntityX#(poly)-EntityX#(backgrnd))*8.5)+537,((EntityZ#(backgrnd)-EntityZ#(poly))*8.6)+88,10,10 ;Player
Color 255,255,255
Oval ((EntityX#(crosshair)-EntityX#(backgrnd))*8.5)+537,((EntityZ#(backgrnd)-EntityZ#(crosshair))*8.6)+88,10,10 ;Player aiming square
Rect 458,8,168,170,0 ;Level Outline

Color 255,0,0
For w.endrone=Each endrone
If w\ingame=True
; While we're looping through all the enemies, we may as well do the AI at the same time (to save on looping again)
	If w\level=playerlevel Then ;As long as the enemy and the player are on the same level.
		Rect (EntityX#(w\sprite)*8.5)+537,(EntityZ#(w\sprite)*-8.6)+88,10,10 ; Draw Enemy square
		If EntityDistance(w\sprite,poly) < 5 And playerdead=0 ;Enemy acts normally, unless player gets withing a certain distance (and is alive)
			;Chase Player (Move the enemytarget To a spot near the player)
			w\action=1
			w\tarx#=EntityX#(poly)-EntityX#(backgrnd)
			w\tarz#=EntityZ#(poly)-EntityZ#(backgrnd)
		Else
			If w\action=0 ;As long as the enemy isn't already chasing the player
				;Act normally
				;If the enemy target is at the target-target (!) coords, create a new target
				If w\x# < w\tarx# + 0.1 And w\z# < w\tarz# + 0.1 And w\x# > w\tarx# - 0.1 And w\z# > w\tarz# - 0.1
					;Set a new Target
					w\tarx#=Rnd(-10,10)
					w\tarz#=Rnd(-10,10)
				EndIf
			Else ;If the enemy is already chasing the player
				w\tarx#=EntityX#(poly)-EntityX#(backgrnd)
				w\tarz#=EntityZ#(poly)-EntityZ#(backgrnd)
			EndIf ;If w\action=0
			texttargetx#=w\tarx#
			texttargetz#=w\tarz#
		EndIf
	Else
		;If enemy and player are on different levels, enemy just acts normally
		If w\x# < w\tarx# + 0.1 And w\z# < w\tarz# + 0.1 And w\x# > w\tarx# - 0.1 And w\z# > w\tarz# - 0.1
			;Set a new Target
			w\tarx#=Rnd(-10,10)
			w\tarz#=Rnd(-10,10)
		EndIf
	EndIf
	
;Move enemy target towards target position (should have been defined above)
If w\x# > w\tarx# Then w\x# = w\x# - enemytarspeed#
If w\x# < w\tarx# Then w\x# = w\x# + enemytarspeed#
If w\z# > w\tarz# Then w\z# = w\z# - enemytarspeed#
If w\z# < w\tarz# Then w\z# = w\z# + enemytarspeed#

;if enemy is on the level, remove health from the level
levlife#(w\level)=levlife#(w\level)-enemyleveldamage#

;position the target and turn the enemy towards it and move it forwards
PositionEntity enemytarget,w\x#,(levely#(w\level)*-1)+0.6,w\z#
PointEntity w\sprite,enemytarget : TurnEntity w\sprite,90,0,0
MoveEntity w\sprite,0,enemyspeed#,0
EndIf ;If w\ingame=True
Next

;if the player is on a level, add health to the level
If levlife#(playerlevel) < 100 Then levlife#(playerlevel)=levlife#(playerlevel)+playerlevelrepair#
;and add health to a level anyway
For n=1 To 4
	;Check the level health levels, and end the game if one of them is below, or equal to, 0
	If levlife#(n) <= 0
		gameover=True
	EndIf
	;Otherwise, heal the level a bit
	If levlife#(n) < 100 Then levlife#(n)=levlife#(n)+levelselfrepair#
Next

;If a screenflash is needed
If screenflash=1
	Color 255,255,255
	Rect 0,0,graphw,graphh
	Color 255,0,0
	screenflash=0
EndIf

;Every ten seconds, update the main game 'timer' (used to increase difficulty)
;Game difficulty theory:
;Add (Diffulcty level * 2) enemies to the game (random level) every 'tick'.
;1 minute (6 ticks) = 42 enemies
If counter1#+10000<MilliSecs()
	PlaySound spawnfx
	gametime=gametime+1
	playerscore=playerscore+12
	;Choose a random position to spawn the enemy (along one of the 4 edges: 1 = far z (x,9), 2 = right x (9,z), 3 = near z (x,-9), 4 = left x (-9,z)
	For m=1 To gametime*2
		spawnside=Rand(1,4) : spawnpoint#=Rnd(1,9) : spawnlevel=Rand(1,4)
		If spawnside=1 Then SpawnEnemy(spawnpoint#,9,spawnlevel,0,1) ;(x#,z#,level,rotation#,enemytype)
		If spawnside=2 Then SpawnEnemy(9,spawnpoint#,spawnlevel,0,1)
		If spawnside=3 Then SpawnEnemy(spawnpoint#,-9,spawnlevel,0,1)
		If spawnside=4 Then SpawnEnemy(-9,spawnpoint#,spawnlevel,0,1)
	Next
	counter1#=MilliSecs()
EndIf

;Level health bars
Color 155,155,155 : Rect 4,4,152,88,0 ;White (well - grey, really) border
If levlife(4) > 65 : Color 0,250,0 : ElseIf levlife(4) > 35 : Color 255,255,0 : ElseIf levlife(4) > 0 : Color 255,0,0 : EndIf
Rect 5,5,levlife#(4)*1.5,20
If levlife(3) > 65 : Color 0,250,0 : ElseIf levlife(3) > 35 : Color 255,255,0 : ElseIf levlife(3) > 0 : Color 255,0,0 : EndIf
Rect 5,27,levlife#(3)*1.5,20
If levlife(2) > 65 : Color 0,250,0 : ElseIf levlife(2) > 35 : Color 255,255,0 : ElseIf levlife(2) > 0 : Color 255,0,0 : EndIf
Rect 5,49,levlife#(2)*1.5,20
If levlife(1) > 65 : Color 0,250,0 : ElseIf levlife(1) > 35 : Color 255,255,0 : ElseIf levlife(1) > 0 : Color 255,0,0 : EndIf
Rect 5,71,levlife#(1)*1.5,20
;Any text
Color 255,255,255
Text 50,7,"Level 4"
Text 50,29,"Level 3"
Text 50,51,"Level 2"
Text 50,73,"Level 1"
Text 5,100,"Bombs:" + nukecount
Text 5,120,"Score:" + playerscore
;Text 0,60,"Level:" + playerlevel
;Text 0,70,"GameTime:" + gametime
;Text 0,80,"ForceField:" + playerprotect
;Text 0,90,"SpeedFactor:" + FL\SpeedFactor
;Text 0,100,"TripleScore:" + triplescore
;Text 0,120,"Transport Position? " + transport
;Text 0,130,"X:" + EntityX#(backgrnd)
;Text 0,140,"Z:" + EntityZ#(backgrnd)

; process bullet flashes in the same way - but no collisions
For f.flash2=Each flash2
	flashdist#=EntityDistance#(poly,f\sprite)
	If flashdist#>2 ;If the flash's position reaches a certain point, delete it
		HideEntity f\sprite
		f\ingame=False
	Else
		MoveEntity f\sprite,0,0,flashspeed#
		ScaleSprite f\sprite,flashdist#/3,flashdist#/3
		EntityAlpha f\sprite,((flashdist#-2)*-1)/2
	EndIf
Next

Flip

Wend

;-------------------
;Game Over routine (entirely seperate from the game code because... it's easier.
;-------------------

PlaySound bombfx

Repeat
If KeyHit(1) Then Goto endgame
If KeyHit(28)
	gameover=False
	Goto startgame
EndIf
RenderWorld
Color 255,255,255
Rect 0,0,graphw,graphh
Color 0,0,0
Text graphw/2,graphh/2,"GAME OVER",True
Text graphw/2,graphh/2+10,"Your score: "+playerscore,True
Text graphw/2,graphh/2+30,"To restart, press ENTER, or ESC to quit",True
Flip
Forever

;-------------------
;Pause routine (entirely seperate from the game code because... it's easier.
;-------------------

.pause
Repeat
If KeyHit(28) Then Goto endgame
If KeyHit(1)
	counter1#=MilliSecs()
	FL\FrameDelay = MilliSecs()
	millifire#=MilliSecs()
	counter1#=MilliSecs()
	tripleguntime#=MilliSecs()
	MoveMouse holdmouseX,holdmouseY
	Goto resumegame
EndIf
RenderWorld
Color 255,255,255
Text GraphicsWidth()/2,GraphicsHeight()/2,"Do you really want to quit?",True
Text GraphicsWidth()/2,GraphicsHeight()/2+10,"ESC = NO",True
Text GraphicsWidth()/2,GraphicsHeight()/2+20,"ENTER = YES",True
Flip
Forever

;-------------------
;End of All Game Loops -----------
;-------------------
.endgame

End

;SubRoutines
;------------
.SetSpeedFactor ;For the framelimiter
   FL\CurrentTicks = MilliSecs()
   FL\SpeedFactor = (FL\CurrentTicks - FL\FrameDelay) / (FL\TicksPerSecond / FL\TargetFPS)
   If FL\SpeedFactor <= 0 Then FL\SpeedFactor = 0.00000000001    
   FL\FPS = FL\TargetFPS / FL\SpeedFactor    
   FL\FrameDelay = FL\CurrentTicks
Return

.changeplayerlevel
	PositionEntity backgrnd,EntityX#(backgrnd),levely#(playerlevel),EntityZ#(backgrnd)
	playerprotect=1 : playerprotectreset=1
Return

;Functions
;------------
Function SpawnEnemy(x#,z#,level,rotation#,enemytype) ;Important! Called whenever a new enemy is needed.
	;Search through the enemies until you find one not currently ingame.
	For q.endrone = Each endrone
   	If q\ingame = False
		y#=(levely#(level)*-1)+0.6 ;Turn the level data into a real position 'PositionEntity' can use
		PositionEntity q\sprite,x#,y#,z# ;: RotateEntity q\sprite,-90,0,rotation# ;Change the x# pos to the input x#
		;If Rand(1,10) < 4 ;Randomly choose enemy type. 0 = wandering, 1 = Chasing
			q\action=0
		;Else
		;	q\action=1
		;EndIf
		q\level=level
		q\ingame = True
		ShowEntity q\sprite
		q\x# = x# : q\z# = z# : q\tarx#= x# : q\tarz# = z# ;Make sure the enemy begins by choosing a random target (if action = 0)
		Exit ;Exit the loop
	EndIf
	Next
End Function

Function levely#(level) ;Returns the y value of the level
	If level=1
		y#=-0.1
	ElseIf level=2
		y#=-5.3
	ElseIf level=3
		y#=-10.3
	ElseIf level=4
		y#=-15.3
	EndIf
	Return y#
End Function

;CreatePoly - for creating a 'sprite' (two polygons put together in a square)

Function CreatePoly(xpos#,zpos#,x#,z#)
	sprite=CreateMesh()
	he=CreateBrush(255,255,255)
	v=CreateSurface(sprite,he)
	FreeBrush he
	AddVertex (v,xpos#,0,zpos#,      0,0)
	AddVertex (v,xpos#+x#,0,zpos#,   1,0)
	AddVertex (v,xpos#+x#,0,zpos#-z#,1,1)
	AddVertex (v,xpos#,0,zpos#-z#,   0,1)
	AddTriangle(v,0,1,2)
	AddTriangle(v,2,3,0)
	AddTriangle(v,2,1,0)
	AddTriangle(v,0,3,2)
	Return sprite
End Function

Function CreateBullet(xpos#,zpos#,x#,z#) ;A bullet is the same as a poly, but without a back (2 less triangles)
	sprite=CreateMesh()
	he=CreateBrush(255,255,255)
	v=CreateSurface(sprite,he)
	FreeBrush he
	AddVertex (v,xpos#,0,zpos#,      0,0)
	AddVertex (v,xpos#+x#,0,zpos#,   1,0)
	AddVertex (v,xpos#+x#,0,zpos#-z#,1,1)
	AddVertex (v,xpos#,0,zpos#-z#,   0,1)
	AddTriangle(v,0,1,2)
	AddTriangle(v,2,3,0)
	Return sprite
End Function

Function CreateFlash() ;We use a bog standard sprite for the flash - we don't need many of them, and they automatically face the camera, which is useful
	sprite=CreateSprite()
	ScaleSprite sprite,0.55,0.55
	Return sprite
End Function

Function BuildMuzzleFlash() ;Fairly specific function to make a muzzle flash shape for the gun
	sprite=CreateMesh()
	he=CreateBrush(255,255,255)
	v=CreateSurface(sprite,he)
	FreeBrush he
	AddVertex (v,-0.5,0.5,0,      0.25,0.75)
	AddVertex (v,0.5,0.5,0,   	  0.75,0.75)
	AddVertex (v,0.5,-0.5,0,	  0.75,0.25)
	AddVertex (v,-0.5,-0.5,0,	  0.25,0.25)
	AddVertex (v,-2,2,1,      0,1)
	AddVertex (v,2,2,1,   	  1,1)
	AddVertex (v,2,-2,1,	  1,0)
	AddVertex (v,-2,-2,1,	  0,0)
	AddTriangle(v,0,1,2)
	AddTriangle(v,0,2,3)
	AddTriangle(v,0,4,5)
	AddTriangle(v,0,5,1)
	AddTriangle(v,1,5,6)
	AddTriangle(v,1,6,2)
	AddTriangle(v,2,6,7)
	AddTriangle(v,2,7,3)
	AddTriangle(v,3,7,4)
	AddTriangle(v,3,4,0)
	AddTriangle(v,1,5,4)
	AddTriangle(v,1,4,0)
	AddTriangle(v,0,4,7)
	AddTriangle(v,0,7,3)
	AddTriangle(v,3,7,6)
	AddTriangle(v,3,6,2)
	AddTriangle(v,2,6,5)
	AddTriangle(v,2,5,1)
	AddTriangle(v,1,0,3)
	AddTriangle(v,1,3,2)
	Return sprite
End Function

Function fire_bullet(x#,z#,angle#)
	;Search through the bullet until you find one not currently ingame.
	For b.bullet = Each bullet
   	If b\ingame = False
		b\x#=x#
		b\z#=z#
		b\angle#=(angle#*-1)-90
		RotateEntity b\sprite,0,b\angle#,0
		PositionEntity b\sprite,b\x#,0.3,b\z#
		EntityParent b\sprite,backgrnd
		b\ingame = True
		ShowEntity b\sprite
		Exit ;Exit the loop
	EndIf
	Next
End Function

Function fire_flash(x#,z#,angle#)
	For f.flash2 = Each flash2
   	If f\ingame = False
		f\x#=x#
		f\z#=z#
		f\angle#=(angle#*-1)-90
		RotateSprite f\sprite,angle#*-1
		RotateEntity f\sprite,0,f\angle#,0
		PositionEntity f\sprite,f\x#,0.3,f\z#
		EntityParent f\sprite,backgrnd
		EntityAlpha f\sprite,1 : ScaleSprite f\sprite,0.55,0.55
		f\ingame = True
		ShowEntity f\sprite
		Exit ;Exit the loop
	EndIf
	Next
End Function

Function fire_shock(x#,y#,z#)
	For sk.shock = Each shock
   	If sk\ingame = False
		PositionEntity sk\sprite,x#,y#-0.69,z#
		sk\ingame = True
		sk\scale#=1
		ScaleEntity sk\sprite,sk\scale#,1,sk\scale#
		EntityAlpha sk\sprite,1
		ShowEntity sk\sprite
		Exit ;Exit the loop
	EndIf
	Next
End Function

Function fire_shockfield(x#,y#,z#)
	For sld.shockfield = Each shockfield
   	If sld\ingame = False
		PositionEntity sld\sprite,x#,y#-0.69,z#
		sld\ingame = True
		sld\scale#=1
		ScaleEntity sld\sprite,sld\scale#,1,sld\scale#
		EntityAlpha sld\sprite,1
		ShowEntity sld\sprite
		Exit ;Exit the loop
	EndIf
	Next
End Function

Function fire_explode(x#,y#,z#)
	For epd.explode = Each explode
   	If epd\ingame = False
		PositionEntity epd\sprite,x#,y#,z#
		epd\ingame = True
		epd\scale#=1
		ScaleSprite epd\sprite,1,1
		EntityAlpha epd\sprite,1
		ShowEntity epd\sprite
		Exit ;Exit the loop
	EndIf
	Next
End Function

Function FrameLimitInit(target_FPS#)
	FL\TargetFPS# = target_FPS#
	FL\TicksPerSecond = 1000 	
	FL\FrameDelay = MilliSecs()
End Function