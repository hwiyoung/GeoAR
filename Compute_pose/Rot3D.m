% 3D rotation matrix
% Impyeong Lee
% University of Seoul
% 2003. 11. 20
% 2004. 11. 28 - [om,ph,kp] -> ra

function R = Rot3D ( ra )

om = ra(1);
ph = ra(2);
kp = ra(3);

Rx = [1 0 0;0 cos(om) sin(om);0 -sin(om) cos(om)];
Ry = [cos(ph) 0 -sin(ph);0 1 0;sin(ph) 0 cos(ph)];
Rz = [cos(kp) sin(kp) 0;-sin(kp) cos(kp) 0; 0 0 1];

R = Rz * Ry * Rx;


  