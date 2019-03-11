function vis_coord_system (pos, rot, len, coord_name, cstr)

for n = 1:3,
    % axis
    pt = pos + len * rot(:,n);
    h = plot3([pos(1), pt(1)], [pos(2), pt(2)], [pos(3), pt(3)], 'r-');
    set(h, 'LineWidth', 3, 'Color', cstr);
    
    % axis name
    h = text( pt(1),  pt(2),  pt(3), sprintf('%c', n+'x'-1) );
    set(h, 'Color', cstr, 'FontWeight', 'bold', 'FontSize', 10, ...
         'VerticalAlignment', 'top', 'HorizontalAlignment', 'center');
end
% coordinate system name
h = text( pos(1),  pos(2),  pos(3), coord_name );
set(h, 'Color', cstr, 'FontWeight', 'bold', 'FontSize', 15, ...
    'VerticalAlignment', 'top', 'HorizontalAlignment', 'center');

